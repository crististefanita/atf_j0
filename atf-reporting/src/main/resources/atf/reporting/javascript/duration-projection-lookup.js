function scenarioDurationLookup() {
  var roots = classItems(), cards = new Map();
  roots.forEach(root => cards.set(String(root.getAttribute('test-id') || ''), directScenarioCards(root)));
  return {roots: roots, cards: cards};
}
function scenarioCardFor(id, index, lookup) {
  var cards = (lookup || scenarioDurationLookup()).cards.get(String(id || ''));
  return cards && (cards[Number(index) || 0] || cards[0]) || null;
}

