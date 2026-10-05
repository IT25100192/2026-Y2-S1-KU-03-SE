/**
 * The only client-side script in the project.
 *
 * Everything else is server-rendered on purpose: six people learning the
 * stack at once can debug a form post far more easily than a front-end
 * state bug, and the assignment is marked on the system, not on the
 * interactivity.
 */
(function () {
  'use strict';

  // Anything destructive asks first. The server still enforces the rule -
  // this only stops an accidental click.
  document.querySelectorAll('form[data-confirm]').forEach(function (form) {
    form.addEventListener('submit', function (event) {
      if (!window.confirm(form.getAttribute('data-confirm'))) event.preventDefault();
    });
  });

  // Stop double submits. A second click on "Cast votes" while the first is
  // in flight would spend the credits twice.
  document.querySelectorAll('form').forEach(function (form) {
    form.addEventListener('submit', function () {
      var button = form.querySelector('button[type=submit], button:not([type])');
      if (!button || form.hasAttribute('data-confirm')) return;
      setTimeout(function () {
        button.disabled = true;
        button.textContent = 'Working...';
      }, 0);
    });
  });

  // Live pages refresh themselves rather than polling an endpoint - one
  // request every 20 seconds, and the server renders what it already knows.
  var live = document.querySelector('[data-refresh]');
  if (live) {
    var seconds = Number(live.getAttribute('data-refresh')) || 20;
    setTimeout(function () { window.location.reload(); }, seconds * 1000);
  }

  // Vote page: keep the cost estimate honest as the count changes.
  var countInput = document.querySelector('[data-vote-count]');
  if (countInput) {
    var freeLeft = Number(countInput.getAttribute('data-free-left')) || 0;
    var price = Number(countInput.getAttribute('data-price')) || 0;
    var out = document.querySelector('[data-vote-cost]');
    var update = function () {
      var n = Math.max(1, Number(countInput.value) || 1);
      var paid = Math.max(0, n - freeLeft);
      out.textContent = paid === 0
        ? 'All ' + n + ' free'
        : (n - paid) + ' free + ' + paid + ' paid (' + paid + ' credit' + (paid === 1 ? '' : 's') + ', LKR ' + (paid * price) + ')';
    };
    countInput.addEventListener('input', update);
    update();
  }
}());
