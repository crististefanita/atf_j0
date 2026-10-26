function q(selector, root) {
  return Array.from((root || document).querySelectorAll(selector));
}
function text(element, selector) {
  var target = selector ? element.querySelector(selector) : element;
  return target ? target.textContent.trim() : '';
}
function classItems() {
  return q('[data-fixture-class]');
}
var timelineUpdates = 0;
var timelineChart = {
  canvas: document.getElementById('timeline'),
  data: {
    datasets: [
      { label: 'Cart', data: [300] },
      { label: 'Checkout', data: [200] }
    ]
  },
  update: function () { timelineUpdates++; }
};
var statusChart = {
  canvas: document.getElementById('status'),
  data: { datasets: [{ label: 'Pass', data: [7, 0] }] },
  update: function () { throw new Error('Duration projection cannot update a status chart'); }
};
var Chart = { instances: { timeline: timelineChart, status: statusChart } };
window.timeline = { Cart: 300, Checkout: 200 };
function overviewDurationSnapshot() {
  normalizeOverviewDuration();
  return {
    total: Number(document.getElementById('timeline').getAttribute('data-duration-ms')),
    source: document.getElementById('timeline').getAttribute('data-duration-source'),
    title: document.getElementById('timeline').getAttribute('title'),
    nativeValues: Object.values(window.timeline),
    chartValues: timelineChart.data.datasets.map(function (dataset) { return dataset.data[0]; }),
    statusValues: statusChart.data.datasets[0].data,
    updates: timelineUpdates
  };
}
function changeOverviewDuration(change) {
  if (change === 'cart') classItems()[0].setAttribute('data-duration-ms', '15000');
  if (change === 'zero') classItems().forEach(function (root) { root.setAttribute('data-duration-ms', '0'); });
  if (change === 'unmeasured') classItems().forEach(function (root) { root.removeAttribute('data-duration-ms'); });
  if (change === 'array-registry') Chart.instances = [timelineChart, statusChart];
  if (change === 'duplicate') classItems()[1].querySelector('.name').textContent = 'Cart';
  return overviewDurationSnapshot();
}
function corruptOverviewDuration(target) {
  if (target === 'marker') document.getElementById('timeline').setAttribute('data-duration-ms', '500000');
  if (target === 'model') window.timeline.Cart = 300;
  if (target === 'chart') timelineChart.data.datasets[0].data[0] = 300;
}
