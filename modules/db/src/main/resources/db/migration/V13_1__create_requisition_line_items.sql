drop table if exists requisition_line_items;
create table requisition_line_items (
id serial primary key,
rnrId int not null references requisition(id),
productCode varchar(50) not null references products(code),
product varchar(250) ,
dispensingUnit VARCHAR(20) not null,
beginningBalance integer,
quantityReceived integer,
quantityDispensed integer,
estimatedConsumption integer,
stockInHand integer,
quantityRequested integer,
reasonForRequestedQuantity text,
calculatedOrderQuantity integer,
quantityApproved integer,
lossesAndAdjustments integer,
reasonForLossesAndAdjustments text,
newPatientCount integer,
stockOutDays integer,
normalizedConsumption integer,
amc numeric(8,4),
maxMonthsOfStock integer NOT NULL,
maxStockQuantity integer,
packsToShip integer,
cost numeric(8, 4),
remarks text,
dosesPerMonth integer not null,
dosesPerDispensingUnit integer not null,
packSize smallint not null,
roundToZero BOOLEAN,
modifiedBy VARCHAR(50),
modifiedDate TIMESTAMP  DEFAULT  CURRENT_TIMESTAMP
);