/*
 * Guess Market web client (exercise 4).
 * Works against the existing Guess Market server with exactly the same endpoints as the JavaFX client.
 * Pull: every 0.5 second the client asks for the data version and fetches the data only when it changed
 * (or when the user selected another event). Each part of the screen is redrawn only if its data changed.
 */
'use strict';

const API = '/guessmarket';
const REFRESH_MILLIS = 500;

const state = {
  user: null,
  version: -1,
  refreshRequested: true,
  refreshing: false,
  timer: null,
  tab: 'events',
  filters: { type: 'All', status: 'All', commission: 'All' },
  onlyMine: false,
  selectedEvent: null,
  selectedUserEvent: null,
  data: { events: [], users: [], me: null, eventDetails: null, userEventDetails: null },
  rendered: {},          // last rendered JSON per screen part (to redraw only on change)
  actionsKey: null,      // the trade forms are rebuilt only when this changes, so typing is never lost
};

// =====================================================================
// Server communication
// =====================================================================

class ServerError extends Error {
  constructor(message, status) {
    super(message);
    this.status = status;
  }
}

async function api(path, { method = 'GET', params = {} } = {}) {
  const query = new URLSearchParams(params).toString();
  const options = { method, credentials: 'same-origin' };
  let url = API + path;
  if (method === 'GET') {
    url += query ? `?${query}` : '';
  } else {
    options.headers = { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' };
    options.body = query;
  }
  let response;
  try {
    response = await fetch(url, options);
  } catch (e) {
    throw new ServerError('Cannot reach the web client server. Is run.bat still running?', 0);
  }
  let body = null;
  try {
    body = await response.json();
  } catch (e) {
    body = null;
  }
  if (!response.ok) {
    const message = body && body.message ? body.message : `Server error (HTTP ${response.status}).`;
    throw new ServerError(message, response.status);
  }
  return body;
}

/** Runs a user action: shows the server's answer and refreshes the data right away. */
async function runAction(path, params, onSuccess) {
  try {
    const result = await api(path, { method: 'POST', params });
    if (onSuccess) onSuccess();
    toast(result.message);
    requestRefresh();
  } catch (e) {
    handleError(e);
  }
}

function handleError(error) {
  if (error.status === 401 && state.user) {
    toast('Your session has ended. Please log in again.', true);
    showLogin();
    return;
  }
  toast(error.message, true);
}

// =====================================================================
// Login / logout
// =====================================================================

const $ = (id) => document.getElementById(id);

$('login-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  const name = $('login-name').value.trim();
  $('login-error').textContent = '';
  if (!name) {
    $('login-error').textContent = 'Please enter a user name.';
    return;
  }
  $('login-button').disabled = true;
  try {
    const result = await api('/login', { method: 'POST', params: { username: name } });
    showMain(result.message); // the server answers with the registered user name
  } catch (e) {
    $('login-error').textContent = e.message;
  } finally {
    $('login-button').disabled = false;
  }
});

$('logout-button').addEventListener('click', async () => {
  try {
    await api('/logout', { method: 'POST' });
  } catch (e) {
    // logging out anyway
  }
  showLogin();
});

// Closing the browser tab logs the user out, so the name is free again
window.addEventListener('pagehide', () => {
  if (state.user) navigator.sendBeacon(`${API}/logout`);
});

function showMain(userName) {
  state.user = userName;
  state.version = -1;
  state.rendered = {};
  state.actionsKey = null;
  state.selectedEvent = null;
  state.selectedUserEvent = null;
  $('header-user').textContent = `Logged in as: ${userName}`;
  $('login-screen').classList.add('hidden');
  $('main-screen').classList.remove('hidden');
  renderEventDetails(null);
  renderUserEvent();
  startRefreshing();
}

function showLogin() {
  state.user = null;
  clearInterval(state.timer);
  $('main-screen').classList.add('hidden');
  $('login-screen').classList.remove('hidden');
  $('login-name').value = '';
  $('login-name').focus();
}

// =====================================================================
// Pull refresh
// =====================================================================

function startRefreshing() {
  clearInterval(state.timer);
  requestRefresh();
  state.timer = setInterval(refresh, REFRESH_MILLIS);
}

function requestRefresh() {
  state.refreshRequested = true;
  refresh();
}

async function refresh() {
  if (!state.user || state.refreshing) return;
  state.refreshing = true;
  try {
    const version = await api('/version');
    if (version !== state.version || state.refreshRequested) {
      state.refreshRequested = false;
      state.version = version;
      await fetchAll();
      renderAll();
    }
    $('connection-status').textContent = '';
  } catch (e) {
    if (e.status === 401) {
      handleError(e);
    } else {
      $('connection-status').textContent = e.message;
    }
  } finally {
    state.refreshing = false;
  }
}

async function fetchAll() {
  const eventName = state.selectedEvent;
  const userEventName = state.selectedUserEvent;
  const [events, users, me, eventDetails, userEventDetails] = await Promise.all([
    api('/events'),
    api('/users'),
    api('/user-details'),
    eventName ? api('/event-details', { params: { event: eventName } }).catch(() => null) : null,
    userEventName ? api('/user-event-details', { params: { event: userEventName } }).catch(() => null) : null,
  ]);
  Object.assign(state.data, { events, users, me, eventDetails, userEventDetails });
}

/** Calls draw() only if the data of this part of the screen changed since the last time. */
function renderIfChanged(part, data, draw) {
  const json = JSON.stringify(data);
  if (state.rendered[part] === json) return;
  state.rendered[part] = json;
  draw(data);
}

function renderAll() {
  const { events, users, me, eventDetails, userEventDetails } = state.data;
  $('header-balance').textContent = `Balance: ${money(me.balance)}`;

  renderIfChanged('events', [events, state.filters, state.selectedEvent], renderEventsTable);
  const details = eventDetails && eventDetails.event.name === state.selectedEvent ? eventDetails : null;
  renderIfChanged('eventDetails', details, renderEventDetails);

  const others = users.filter((u) => u.name.toLowerCase() !== state.user.toLowerCase());
  renderIfChanged('users', others, renderUsersTable);
  renderIfChanged('account', [me.balance, me.blocked, me.transactions], renderAccount);
  renderIfChanged('userEvents', [me.events, state.onlyMine, state.selectedUserEvent], renderUserEventsTable);
  renderIfChanged('userEvent', [userEventDetails, state.selectedUserEvent], renderUserEvent);
}

// =====================================================================
// Tabs and filters
// =====================================================================

document.querySelectorAll('.tab').forEach((tab) => tab.addEventListener('click', () => {
  state.tab = tab.dataset.tab;
  document.querySelectorAll('.tab').forEach((t) => t.classList.toggle('active', t === tab));
  $('tab-events').classList.toggle('hidden', state.tab !== 'events');
  $('tab-account').classList.toggle('hidden', state.tab !== 'account');
}));

document.querySelectorAll('.filter-row').forEach((row) => {
  row.querySelectorAll('.chip').forEach((chip) => chip.addEventListener('click', () => {
    row.querySelectorAll('.chip').forEach((c) => c.classList.toggle('selected', c === chip));
    state.filters[row.dataset.filter] = chip.dataset.value;
    renderAll();
  }));
});

$('only-mine').addEventListener('change', (e) => {
  state.onlyMine = e.target.checked;
  renderAll();
});

function matchesFilters(event) {
  const { type, status, commission } = state.filters;
  return (type === 'All' || event.type === type)
    && (status === 'All' || event.status === status)
    && (commission === 'All' || event.commissionType === commission);
}

// =====================================================================
// Events tab
// =====================================================================

function renderEventsTable() {
  const rows = state.data.events.filter(matchesFilters);
  $('events-table').innerHTML = header(['Name', 'Type', 'Status', 'Commission', 'Event Account', 'Market Maker'])
    + body(rows, (e) => `<tr class="clickable ${e.name === state.selectedEvent ? 'selected' : ''}" data-event="${esc(e.name)}">
        <td>${esc(e.name)}</td><td>${esc(e.type)}</td><td>${esc(e.status)}</td>
        <td>${e.commission}% (${esc(e.commissionType)})</td><td class="num">${money(e.accountBalance)}</td>
        <td>${esc(e.marketMaker)}</td></tr>`, 6, 'No events yet. Upload an XML file from the JavaFX client to add events.');
}

$('events-table').addEventListener('click', (e) => {
  const row = e.target.closest('tr[data-event]');
  if (!row) return;
  state.selectedEvent = row.dataset.event;
  renderAll();
  renderEventDetails('loading');
  requestRefresh();
});

function renderEventDetails(details) {
  const container = $('event-details');
  if (details === 'loading') {
    state.rendered.eventDetails = null;
    container.innerHTML = `<p class="muted">Loading '${esc(state.selectedEvent)}'...</p>`;
    return;
  }
  if (!details) {
    container.innerHTML = '<p class="muted">Select an event to see its details.</p>';
    return;
  }
  const e = details.event;
  const isLmsr = e.type === 'LMSR';
  container.innerHTML = `
    <h2>${esc(e.name)}</h2>
    <p>${esc(e.description)}</p>
    <dl class="info-grid">
      <dt>Method:</dt><dd>${esc(e.type)}</dd>
      <dt>Status:</dt><dd>${esc(e.status)}</dd>
      <dt>Commission:</dt><dd>${e.commission}% (${esc(e.commissionType)})</dd>
      <dt>Market Maker:</dt><dd>${esc(e.marketMaker)}</dd>
      <dt>Event Account:</dt><dd>${money(e.accountBalance)}</dd>
      <dt>Commission Collected:</dt><dd>${money(details.totalCommission)}</dd>
      <dt>Winning Option:</dt><dd>${esc(details.winningOption || '-')}</dd>
    </dl>
    ${isLmsr ? lmsrDetailsHtml(details) : orderBooksHtml(details.orderBooks)}
    <h3>Participants</h3>
    ${table(['Name', 'Holdings (shares / value)', 'Open Orders', 'Total Value'], details.participants, (p) => `<tr>
        <td>${esc(p.name)}</td>
        <td>${p.holdings.map((h) => `${esc(h.option)}: ${h.shares} (${money(h.value)})`).join(' &nbsp;|&nbsp; ')}</td>
        <td class="num">${p.openOrders}</td><td class="num">${money(p.totalValue)}</td></tr>`, 'No participants yet.')}`;
}

function lmsrDetailsHtml(details) {
  return `
    <h3>Current State</h3>
    ${table(['Option', 'Current Value', 'Shares Bought'], details.lmsrOptions, (o) => `<tr>
        <td>${esc(o.option)}</td><td class="num">${money(o.price)}</td><td class="num">${o.sharesBought}</td></tr>`)}
    <h3>Trade History (latest first)</h3>
    ${table(['User', 'Option', 'Shares', 'Price Paid', 'Commission'], details.lmsrTrades, (t) => `<tr>
        <td>${esc(t.userName)}</td><td>${esc(t.option)}</td><td class="num">${t.shares}</td>
        <td class="num">${money(t.pricePaid)}</td><td class="num">${money(t.commission)}</td></tr>`, 'No trades yet.')}`;
}

function orderBooksHtml(books) {
  const ordersTable = (orders) => table(['User', 'Shares', 'Price per Share'], orders, (o) => `<tr>
      <td>${esc(o.userName)}</td><td class="num">${o.shares}</td><td class="num">${money(o.price)}</td></tr>`, 'No orders');
  return `
    <h3>Order Books (one per option)</h3>
    <div class="books">${books.map((b) => `
      <div class="card">
        <h2>Option: ${esc(b.option)}</h2>
        <div class="stats">LAST: ${moneyOrNA(b.last)} &nbsp; BID: ${moneyOrNA(b.bid)} &nbsp; ASK: ${moneyOrNA(b.ask)}
          <br>MID: ${moneyOrNA(b.mid)} &nbsp; SPREAD: ${moneyOrNA(b.spread)}</div>
        <h3>Buy orders (bids)</h3>${ordersTable(b.bids)}
        <h3>Sell orders (asks)</h3>${ordersTable(b.asks)}
      </div>`).join('')}
    </div>`;
}

// =====================================================================
// Account tab
// =====================================================================

function renderUsersTable(users) {
  $('users-table').innerHTML = header(['User Name', 'Balance', 'Market Maker', 'Online'])
    + body(users, (u) => `<tr><td>${esc(u.name)}</td><td class="num">${money(u.balance)}</td>
        <td>${u.marketMaker ? 'Yes' : 'No'}</td><td>${u.online ? 'Yes' : 'No'}</td></tr>`, 4, 'No other users are registered yet.');
}

function renderAccount() {
  const me = state.data.me;
  $('account-balance').textContent = `Balance: ${money(me.balance)}`;
  const status = $('account-status');
  status.textContent = me.blocked ? 'BLOCKED (negative balance)' : 'Active';
  status.className = me.blocked ? 'negative' : 'positive';
  const latestFirst = [...me.transactions].reverse();
  $('transactions-table').innerHTML = header(['#', 'Time', 'Type', 'Description', 'Amount', 'Balance Before', 'Balance After'])
    + body(latestFirst, (t) => `<tr><td>${t.number}</td><td>${esc(t.time)}</td><td>${esc(t.type)}</td>
        <td>${esc(t.description)}</td><td class="num ${t.amount < 0 ? 'negative' : 'positive'}">${money(t.amount)}</td>
        <td class="num">${money(t.balanceBefore)}</td><td class="num">${money(t.balanceAfter)}</td></tr>`,
    7, "No transactions yet. Use 'Load Funds' to add money.");
}

$('deposit-form').addEventListener('submit', (e) => {
  e.preventDefault();
  const amount = parsePositive($('deposit-amount').value, false, 'The amount');
  if (amount !== null) {
    runAction('/deposit', { amount }, () => { $('deposit-amount').value = ''; });
  }
});

function renderUserEventsTable() {
  const rows = state.data.me.events.filter((e) => !state.onlyMine || e.role !== '-');
  $('user-events-table').innerHTML = header(['Event', 'Type', 'Status', 'My Role'])
    + body(rows, (e) => `<tr class="clickable ${e.eventName === state.selectedUserEvent ? 'selected' : ''}" data-event="${esc(e.eventName)}">
        <td>${esc(e.eventName)}</td><td>${esc(e.type)}</td><td>${esc(e.status)}</td><td>${esc(e.role)}</td></tr>`,
    4, 'No events.');
}

$('user-events-table').addEventListener('click', (e) => {
  const row = e.target.closest('tr[data-event]');
  if (!row || row.dataset.event === state.selectedUserEvent) return;
  state.selectedUserEvent = row.dataset.event;
  state.data.userEventDetails = null;
  renderAll();
  requestRefresh();
});

function renderUserEvent() {
  const d = state.data.userEventDetails;
  const valid = d && d.eventName === state.selectedUserEvent;
  if (!valid) {
    $('user-event-header').innerHTML = state.selectedUserEvent
      ? `<p class="muted">Loading '${esc(state.selectedUserEvent)}'...</p>`
      : '<p class="muted">Select an event to see your involvement in it and to trade.</p>';
    $('user-event-actions').innerHTML = '';
    $('user-event-data').innerHTML = '';
    state.actionsKey = null;
    return;
  }
  $('user-event-header').innerHTML = `<h2>${esc(d.eventName)}</h2>
    <p>${esc(d.type)} &nbsp;|&nbsp; ${esc(d.status)}${d.marketMaker ? ' &nbsp;|&nbsp; You are the market maker' : ''}</p>`;
  renderUserEventActions(d);
  renderUserEventData(d);
}

/** The action forms. Rebuilt only when the event / its status changes, so what the user typed stays. */
function renderUserEventActions(d) {
  const key = JSON.stringify([d.eventName, d.status, d.marketMaker, d.options]);
  if (key === state.actionsKey) return;
  state.actionsKey = key;

  const active = d.status === 'Active';
  const optionList = d.options.map((o) => `<option value="${esc(o)}">${esc(o)}</option>`).join('');
  const disabled = active ? '' : 'disabled';
  let html = '';
  if (d.marketMaker) {
    html += `<form id="mm-form" class="card inline-form">
      <strong>Market maker actions</strong>
      <button type="button" id="activate-button" class="btn primary" ${d.status === 'Not Started' ? '' : 'disabled'}>Activate Event</button>
      <select id="winner-select" ${disabled}><option value="">Winning option</option>${optionList}</select>
      <button type="submit" class="btn primary" ${disabled}>Close Event</button>
    </form>`;
  }
  if (d.type === 'LMSR') {
    html += `<form id="lmsr-form" class="card inline-form">
      <strong>Buy shares (LMSR)</strong>
      <select id="lmsr-option" ${disabled}><option value="">Option</option>${optionList}</select>
      <input id="lmsr-shares" type="text" inputmode="numeric" placeholder="Shares" ${disabled}>
      <button type="submit" class="btn primary" ${disabled}>Buy</button>
    </form>`;
  } else {
    html += `<form id="order-form" class="card inline-form">
      <strong>Place an order (Order Book)</strong>
      <select id="order-side" ${disabled}><option value="buy">Buy</option><option value="sell">Sell</option></select>
      <select id="order-option" ${disabled}><option value="">Option</option>${optionList}</select>
      <input id="order-shares" type="text" inputmode="numeric" placeholder="Shares" ${disabled}>
      <input id="order-price" type="text" inputmode="decimal" placeholder="Price / share" ${disabled}>
      <button type="submit" class="btn primary" ${disabled}>Submit Order</button>
    </form>`;
  }
  $('user-event-actions').innerHTML = html;
  bindUserEventActions(d.eventName);
}

function bindUserEventActions(eventName) {
  const activate = $('activate-button');
  if (activate) {
    activate.addEventListener('click', () => runAction('/activate-event', { event: eventName }));
    $('mm-form').addEventListener('submit', (e) => {
      e.preventDefault();
      const winner = $('winner-select').value;
      if (!winner) return toast('Please choose the winning option.', true);
      runAction('/close-event', { event: eventName, winner });
    });
  }
  const lmsrForm = $('lmsr-form');
  if (lmsrForm) {
    lmsrForm.addEventListener('submit', (e) => {
      e.preventDefault();
      const option = $('lmsr-option').value;
      if (!option) return toast('Please choose an option.', true);
      const shares = parsePositive($('lmsr-shares').value, true, 'The number of shares');
      if (shares !== null) {
        runAction('/buy-shares', { event: eventName, option, shares }, () => { $('lmsr-shares').value = ''; });
      }
    });
  }
  const orderForm = $('order-form');
  if (orderForm) {
    orderForm.addEventListener('submit', (e) => {
      e.preventDefault();
      const option = $('order-option').value;
      if (!option) return toast('Please choose an option.', true);
      const shares = parsePositive($('order-shares').value, true, 'The number of shares');
      if (shares === null) return;
      const price = parsePositive($('order-price').value, false, 'The price per share');
      if (price === null) return;
      runAction('/place-order', { event: eventName, option, side: $('order-side').value, shares, price }, () => {
        $('order-shares').value = '';
        $('order-price').value = '';
      });
    });
  }
}

function renderUserEventData(d) {
  const isLmsr = d.type === 'LMSR';
  const pl = d.profitLoss === null || d.profitLoss === undefined
    ? 'Available when the event ends'
    : `<span class="${d.profitLoss < 0 ? 'negative' : 'positive'}">${money(d.profitLoss)}</span>`;
  $('user-event-data').innerHTML = `
    <dl class="info-grid">
      <dt>Commission I paid:</dt><dd>${money(d.commissionPaid)}</dd>
      <dt>My profit / loss:</dt><dd>${pl}</dd>
      <dt>Winning option:</dt><dd>${esc(d.winningOption || '-')}</dd>
    </dl>
    <h3>My holdings</h3>
    ${table(['Option', 'Shares Held', 'Amount Paid', 'Current Value'], d.holdings, (h) => `<tr>
        <td>${esc(h.option)}</td><td class="num">${h.shares}</td><td class="num">${money(h.amountPaid)}</td>
        <td class="num">${money(h.value)}</td></tr>`)}
    ${isLmsr ? `
      <h3>My trades in this event (latest first)</h3>
      ${table(['Option', 'Shares', 'Price Paid', 'Commission'], d.trades, (t) => `<tr>
          <td>${esc(t.option)}</td><td class="num">${t.shares}</td><td class="num">${money(t.pricePaid)}</td>
          <td class="num">${money(t.commission)}</td></tr>`, 'No trades yet.')}
      <h3>Total shares per option</h3>
      ${table(['Option', 'Current Value', 'Total Shares Bought (all users)'], d.totalsPerOption, (o) => `<tr>
          <td>${esc(o.option)}</td><td class="num">${money(o.price)}</td><td class="num">${o.sharesBought}</td></tr>`)}`
    : `
      <h3>My waiting orders</h3>
      ${table(['Side', 'Option', 'Shares Left', 'Price per Share'], d.openOrders, (o) => `<tr>
          <td>${o.buy ? 'Buy' : 'Sell'}</td><td>${esc(o.option)}</td><td class="num">${o.shares}</td>
          <td class="num">${money(o.price)}</td></tr>`, 'No waiting orders.')}`}`;
}

// =====================================================================
// Helpers
// =====================================================================

function esc(value) {
  return String(value ?? '').replace(/[&<>"']/g, (c) => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;',
  }[c]));
}

function money(value) {
  return Number(value).toFixed(2);
}

function moneyOrNA(value) {
  return value === null || value === undefined ? 'N/A' : money(value);
}

function header(titles) {
  return `<thead><tr>${titles.map((t) => `<th>${esc(t)}</th>`).join('')}</tr></thead>`;
}

function body(rows, rowHtml, columns, emptyText = 'No data') {
  if (!rows || rows.length === 0) {
    return `<tbody><tr><td class="empty" colspan="${columns}">${esc(emptyText)}</td></tr></tbody>`;
  }
  return `<tbody>${rows.map(rowHtml).join('')}</tbody>`;
}

function table(titles, rows, rowHtml, emptyText) {
  return `<div class="table-wrap"><table class="data-table">${header(titles)}${body(rows, rowHtml, titles.length, emptyText)}</table></div>`;
}

/** Validates a positive number typed by the user; shows an error and returns null if it is not valid. */
function parsePositive(text, wholeNumber, what) {
  const trimmed = String(text).trim();
  const valid = wholeNumber ? /^\d+$/.test(trimmed) : /^\d+(\.\d+)?$/.test(trimmed);
  const value = Number(trimmed);
  if (!valid || value <= 0) {
    toast(`${what} must be a positive ${wholeNumber ? 'whole number' : 'number'}.`, true);
    return null;
  }
  return trimmed;
}

function toast(message, isError = false) {
  const element = document.createElement('div');
  element.className = `toast ${isError ? 'error' : ''}`;
  element.textContent = message;
  $('toasts').appendChild(element);
  setTimeout(() => element.remove(), isError ? 6000 : 4000);
}
