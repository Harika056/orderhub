-- One database per service. Services never read each other's tables.
CREATE DATABASE orderdb;
CREATE DATABASE inventorydb;
CREATE DATABASE paymentdb;
CREATE DATABASE notificationdb;
