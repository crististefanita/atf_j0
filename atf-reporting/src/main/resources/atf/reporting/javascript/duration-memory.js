var atfMeasuredDurationsByNodeId = Object.create(null);
var atfMeasuredDurationsByPath = Object.create(null);
function rememberMeasuredDurations() {
  q('#test-view .test-list-item .test-contents .card > .card-header').forEach(
    function (header) {
      var card = header.parentElement;
      if (!card) return;
      var badge =
          header.querySelector('.node-info .badge-default') ||
          header.querySelector('.card-toolbar .badge-default') ||
          header.querySelector('.card-toolbar .badge'),
        value = atfDurationValue(text(badge, ''));
      if (value === null) return;
      var node = directNode(card),
        id = nodeIdentity(card),
        path = nodePathKey(card);
      if (id && atfMeasuredDurationsByNodeId[id] === undefined)
        atfMeasuredDurationsByNodeId[id] = value;
      if (path && atfMeasuredDurationsByPath[path] === undefined)
        atfMeasuredDurationsByPath[path] = value;
      [node, card].forEach(e => {
        if (e && e.getAttribute("data-atf-measured-duration-ms") === null)
          e.setAttribute("data-atf-measured-duration-ms", String(value));
      });
      durationAttr(badge, "data-atf-measured-duration-ms", value);
    }
  );
}
function measuredNodeDuration(card) {
  var marked = [directNode(card), card]
    .map(e => e && e.getAttribute("data-atf-measured-duration-ms")).find(v => v != null && v !== "");
  if (marked !== undefined) return Number(marked);
  var remembered = [atfMeasuredDurationsByNodeId[nodeIdentity(card)],
    atfMeasuredDurationsByPath[nodePathKey(card)]].find(v => v !== undefined);
  if (remembered !== undefined) return Number(remembered);
  var badge = durationBadge(card),
    badgeMarked = badge && badge.getAttribute('data-atf-measured-duration-ms'),
    value =
      badgeMarked !== null ? Number(badgeMarked) : atfDurationValue(text(badge, ''));
  if (card && value !== null)
    card.setAttribute('data-atf-measured-duration-ms', String(value));
  return value;
}
