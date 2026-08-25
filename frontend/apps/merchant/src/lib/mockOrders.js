export const MOCK_ORDERS = [
    {
        id: 'ord_1001',
        customerId: 'cust_aa11',
        currentStatus: 'DELIVERED',
        totalAmountMinor: 24000,
        discountAmountMinor: 2000,
        finalAmountMinor: 22000,
        items: [
            { productId: 'p_1', quantity: 2, priceAtPurchaseMinor: 12000 },
        ],
    },
    {
        id: 'ord_1002',
        customerId: 'cust_bb22',
        currentStatus: 'SHIPPED',
        totalAmountMinor: 8500,
        discountAmountMinor: 0,
        finalAmountMinor: 8500,
        items: [
            { productId: 'p_2', quantity: 1, priceAtPurchaseMinor: 8500 },
        ],
    },
    {
        id: 'ord_1003',
        customerId: 'cust_cc33',
        currentStatus: 'CONFIRMED',
        totalAmountMinor: 15000,
        discountAmountMinor: 1500,
        finalAmountMinor: 13500,
        items: [
            { productId: 'p_3', quantity: 3, priceAtPurchaseMinor: 5000 },
        ],
    },
    {
        id: 'ord_1004',
        customerId: 'cust_dd44',
        currentStatus: 'PENDING',
        totalAmountMinor: 4200,
        discountAmountMinor: 0,
        finalAmountMinor: 4200,
        items: [
            { productId: 'p_4', quantity: 1, priceAtPurchaseMinor: 4200 },
        ],
    },
    {
        id: 'ord_1005',
        customerId: 'cust_ee55',
        currentStatus: 'CANCELLED',
        totalAmountMinor: 9900,
        discountAmountMinor: 0,
        finalAmountMinor: 9900,
        items: [
            { productId: 'p_5', quantity: 1, priceAtPurchaseMinor: 9900 },
        ],
    },
];
