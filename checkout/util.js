/**
 * Prints the given error message.
 * @param {string} msg - The error message to print.
 */
function error(msg) {  // eslint-disable-line no-unused-vars
  let element = document.createElement('pre');
  element.innerHTML = msg;
  element.className = 'error';
  document.getElementById('msg').appendChild(element);
  dismissPageDimmer();
}

/**
 * Prints the given informational message.
 * @param {string} msg - The information message to print.
 */
function info(msg) {
  let element = document.createElement('pre');
  element.innerHTML = msg;
  element.className = 'info';
  document.getElementById('msg').appendChild(element);
}

/**
 * Clears all messages.
 */
function clearAllMessages() {  // eslint-disable-line no-unused-vars
  document.getElementById('msg').innerHTML = '';
}

/**
 * Shows a full page half-transparent overlay.
 */
function showPageDimmer() {
  const dimmer = document.createElement('div');
  dimmer.id = 'dimmer';
  dimmer.style = 'position: fixed; padding: 0; margin: 0; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0, 0, 0, 0.5); z-index: 1;';
  document.body.appendChild(dimmer);
}

/**
 * Removes the overlay.
 */
function dismissPageDimmer() {
  const dimmer = document.getElementById('dimmer');
  if (dimmer) {
    dimmer.remove();
  }
}
