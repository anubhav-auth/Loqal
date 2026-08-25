// MOCK ADMIN DATA — local placeholder data (no dedicated admin endpoint client exists yet).
// TODO: replace with calls to a real admin API when available.
export const MOCK_MERCHANTS = [
    {
        id: 'm_001',
        name: 'Saffron & Sage',
        email: 'hello@saffronandsage.in',
        status: 'pending',
        joinedAt: '2026-08-12',
    },
    {
        id: 'm_002',
        name: 'The Loom Collective',
        email: 'team@theloom.co',
        status: 'active',
        joinedAt: '2026-07-03',
    },
    {
        id: 'm_003',
        name: 'Clayworks Studio',
        email: 'hi@clayworks.studio',
        status: 'pending',
        joinedAt: '2026-08-19',
    },
    {
        id: 'm_004',
        name: 'Northwind Goods',
        email: 'orders@northwindgoods.com',
        status: 'active',
        joinedAt: '2026-06-21',
    },
    {
        id: 'm_005',
        name: 'Verde Pantry',
        email: 'care@verdepantry.in',
        status: 'pending',
        joinedAt: '2026-08-22',
    },
    {
        id: 'm_006',
        name: 'Lumen Atelier',
        email: 'studio@lumenatelier.com',
        status: 'active',
        joinedAt: '2026-05-30',
    },
];
export const MOCK_AUDIT = [
    {
        id: 'a_001',
        timestamp: '2026-08-24T09:42:00Z',
        actor: 'admin@loqal.in',
        action: 'MERCHANT_APPROVED',
        target: 'The Loom Collective',
    },
    {
        id: 'a_002',
        timestamp: '2026-08-24T08:15:00Z',
        actor: 'admin@loqal.in',
        action: 'ORDER_FLAGGED',
        target: 'ord_1042',
    },
    {
        id: 'a_003',
        timestamp: '2026-08-23T17:05:00Z',
        actor: 'admin@loqal.in',
        action: 'COUPON_CREATED',
        target: 'WELCOME10',
    },
    {
        id: 'a_004',
        timestamp: '2026-08-23T11:30:00Z',
        actor: 'admin@loqal.in',
        action: 'AGENT_INVITED',
        target: 'ops@loqal.in',
    },
    {
        id: 'a_005',
        timestamp: '2026-08-22T14:48:00Z',
        actor: 'admin@loqal.in',
        action: 'MERCHANT_REJECTED',
        target: 'QuickMart',
    },
    {
        id: 'a_006',
        timestamp: '2026-08-22T10:02:00Z',
        actor: 'admin@loqal.in',
        action: 'SETTINGS_UPDATED',
        target: 'Payout schedule',
    },
    {
        id: 'a_007',
        timestamp: '2026-08-21T16:20:00Z',
        actor: 'admin@loqal.in',
        action: 'MERCHANT_APPROVED',
        target: 'Northwind Goods',
    },
];
export const MOCK_METRICS = {
    totalMerchants: 142,
    totalOrders: 3874,
    gmvMinor: 96450000, // ₹9,64,500.00
    activeAgents: 9,
};
// Orders per day (mock) for the bar chart.
export const MOCK_ORDERS_PER_DAY = [
    { day: 'Mon', orders: 412 },
    { day: 'Tue', orders: 521 },
    { day: 'Wed', orders: 388 },
    { day: 'Thu', orders: 604 },
    { day: 'Fri', orders: 712 },
    { day: 'Sat', orders: 658 },
    { day: 'Sun', orders: 579 },
];
