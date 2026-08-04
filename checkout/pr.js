let readinessGeneration = 0;
let readyRequest = null;

function getAmount() {
  const urlParams = new URLSearchParams(window.location.search);
  return urlParams.get('amount') || '0.01';
}

function setInvokeEnabled(enabled) {
  document.getElementById('invokePaymentButton').disabled = !enabled;
}

function setCheckEnabled(enabled) {
  document.getElementById('checkPaymentButton').disabled = !enabled;
}

function invalidateReadiness() {
  readinessGeneration += 1;
  readyRequest = null;
  setInvokeEnabled(false);
  return readinessGeneration;
}

/**
 * Builds one PaymentRequest for an optional readiness override.
 * @param {string} returnValue - The IS_READY_TO_PAY override, or empty for app default
 * @param {string} amount - The payment amount
 * @return {PaymentRequest|null} The payment request object.
 */
function buildPaymentRequest(returnValue, amount = '0.01') {
  if (!window.PaymentRequest) {
    error('Payment Request API is not supported or not enabled.');
    return null;
  }

  const data = {testField: 'test value'};
  if (returnValue !== '') {
    data.returnValue = returnValue;
  }

  const supportedInstruments = [{
    supportedMethods: 'https://pay-nine-tan.vercel.app/payment-method',
    data,
  }];

  const details = {
    total: {
      label: 'Total',
      amount: {
        currency: 'USD',
        value: amount,
      },
    },
  };

  let request;

  try {
    request = new PaymentRequest(supportedInstruments, details);
  } catch (e) {
    error(`Readiness [mode=${describeReadinessMode(returnValue)}]: PaymentRequest construction failed: ${e}`);
    return null;
  }

  request.addEventListener('paymentmethodchange', e => {
    info('"paymentmethodchange" called on request with method name ' + e.methodName);
    info('Responding with an error, for testing');
    e.updateWith({error: 'Error for testing'});
  });

  return request;
}

function settleProbe(promise) {
  return Promise.resolve(promise).then(
    value => ({value: Boolean(value)}),
    reason => ({error: reason}),
  );
}

function runProbe(probe) {
  try {
    return settleProbe(probe());
  } catch (reason) {
    return Promise.resolve({error: reason});
  }
}

function describeError(reason) {
  if (reason && reason.name && reason.message) {
    return `${reason.name}: ${reason.message}`;
  }
  return String(reason);
}

function describeReadinessMode(returnValue) {
  return returnValue === '' ? 'app-default' : `override-${returnValue}`;
}

/**
 * Invalidates readiness when the override changes. Probing is user initiated.
 */
function onReturnValueChanged() { // eslint-disable-line no-unused-vars
  invalidateReadiness();
  const returnValue = document.getElementById('returnValue').value;
  setCheckEnabled(true);
  info(`Readiness [mode=${describeReadinessMode(returnValue)}]: click Check to create and verify a fresh PaymentRequest.`);
}

/**
 * Creates and probes one fresh request for the selected readiness mode.
 */
function onCheckClicked() { // eslint-disable-line no-unused-vars
  const generation = invalidateReadiness();
  const returnValue = document.getElementById('returnValue').value;
  const mode = describeReadinessMode(returnValue);

  setCheckEnabled(false);
  const amount = getAmount();
  info(`Readiness [mode=${mode}]: constructing one fresh PaymentRequest for USD ${amount}.`);
  const candidate = buildPaymentRequest(returnValue, amount);
  if (!candidate) {
    setCheckEnabled(true);
    return;
  }

  if (typeof candidate.canMakePayment !== 'function') {
    error(`Readiness [mode=${mode}]: canMakePayment() is unavailable; failing closed.`);
    setCheckEnabled(true);
    return;
  }

  if (typeof candidate.hasEnrolledInstrument !== 'function') {
    error(`Readiness [mode=${mode}]: hasEnrolledInstrument() is unavailable; Chromium IS_READY_TO_PAY cannot be verified, so readiness fails closed.`);
    setCheckEnabled(true);
    return;
  }

  info(`Readiness [mode=${mode}]: checking canMakePayment() and hasEnrolledInstrument().`);
  const canMakePayment = runProbe(() => candidate.canMakePayment());
  const hasEnrolledInstrument = runProbe(() => candidate.hasEnrolledInstrument());

  Promise.all([canMakePayment, hasEnrolledInstrument]).then(results => {
    if (generation !== readinessGeneration) {
      return;
    }

    const canMakePaymentResult = results[0];
    const enrolledInstrumentResult = results[1];

    if (canMakePaymentResult.error) {
      error(`Readiness [mode=${mode}]: canMakePayment() failed (${describeError(canMakePaymentResult.error)}); failing closed.`);
    } else {
      info(`Readiness [mode=${mode}]: canMakePayment=${canMakePaymentResult.value}.`);
    }

    if (enrolledInstrumentResult.error) {
      const reason = enrolledInstrumentResult.error;
      if (reason && reason.name === 'NotAllowedError') {
        error(`Readiness [mode=${mode}]: hasEnrolledInstrument() failed with NotAllowedError. Chromium limits different methodData query shapes per origin for 30 minutes; failing closed.`);
      } else {
        error(`Readiness [mode=${mode}]: hasEnrolledInstrument() failed (${describeError(reason)}); failing closed.`);
      }
    } else {
      info(`Readiness [mode=${mode}]: hasEnrolledInstrument=${enrolledInstrumentResult.value}.`);
    }

    if (canMakePaymentResult.error || enrolledInstrumentResult.error ||
        !canMakePaymentResult.value || !enrolledInstrumentResult.value) {
      error(`Readiness [mode=${mode}]: not ready; Invoke Payment App remains disabled.`);
      setCheckEnabled(true);
      return;
    }

    readyRequest = candidate;
    setInvokeEnabled(true);
    info(`Readiness [mode=${mode}]: passed; Invoke Payment App now uses this exact verified request.`);
  });
}

function finishPaymentAttempt() {
  dismissPageDimmer();
  document.getElementById('returnValue').value = '';
  setCheckEnabled(true);
  info('Payment attempt settled. App-default mode restored; click Check to verify a new request.');
}

/**
 * Handles the response from PaymentRequest.show().
 * @param {PaymentResponse} response - The accepted payment response
 */
function handlePaymentResponse(response) {
  response.complete('success')
    .then(function() {
      info(JSON.stringify(response, undefined, 2));
      finishPaymentAttempt();
    })
    .catch(function(err) {
      error(err);
      finishPaymentAttempt();
    });
}

/**
 * Shows the exact request that passed readiness checks.
 */
function onBuyClicked() { // eslint-disable-line no-unused-vars
  if (!readyRequest) {
    error('No verified PaymentRequest is ready. Click Check and wait for both readiness checks to pass.');
    return;
  }

  const requestToShow = readyRequest;
  invalidateReadiness();
  showPageDimmer();

  let showPromise;
  try {
    // Keep show() directly in the click event stack to preserve transient activation.
    showPromise = requestToShow.show();
  } catch (e) {
    error(`PaymentRequest.show() failed synchronously: ${e}`);
    finishPaymentAttempt();
    return;
  }

  showPromise
    .then(handlePaymentResponse)
    .catch(function(err) {
      error(err);
      finishPaymentAttempt();
    });
}
