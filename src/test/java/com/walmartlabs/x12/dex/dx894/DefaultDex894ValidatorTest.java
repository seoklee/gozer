/**
Copyright (c) 2018-present, Walmart, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package com.walmartlabs.x12.dex.dx894;

import com.walmartlabs.x12.exceptions.X12ErrorDetail;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class DefaultDex894ValidatorTest {

    DefaultDex894Validator dexValidator;

    @Before
    public void init() {
        dexValidator = new DefaultDex894Validator();
    }

    @Test
    public void test_validate() {
        Dex894 dex = new Dex894();
        dex.setNumberOfTransactions(5);
        dex.setTransactions(this.generateTransactions(5));

        Set<X12ErrorDetail> errorSet = dexValidator.validate(dex);
        assertNotNull(errorSet);
        assertEquals(0, errorSet.size());
    }

    @Test
    public void test_validate_transactionCountWrong() {
        Dex894 dex = new Dex894();
        dex.setNumberOfTransactions(2);
        dex.setTransactions(this.generateTransactions(5));

        Set<X12ErrorDetail> errorSet = dexValidator.validate(dex);
        assertNotNull(errorSet);
        assertEquals(1, errorSet.size());
        assertEquals("DXE", errorSet.stream().findFirst().get().getSegmentId());
    }

    @Test
    public void test_validate_segmentCountWrong() {
        Dex894 dex = new Dex894();
        dex.setNumberOfTransactions(5);
        dex.setTransactions(this.generateTransactions(5));
        dex.getTransactions().get(2).setActualNumberOfSegments(2);
        dex.getTransactions().get(2).setExpectedNumberOfSegments(8);

        Set<X12ErrorDetail> errorSet = dexValidator.validate(dex);
        assertNotNull(errorSet);
        assertEquals(1, errorSet.size());
        assertEquals("SE", errorSet.stream().findFirst().get().getSegmentId());
    }

    @Test
    public void test_validate_missingSupplierNumber_and_duplicates() {
        Dex894 dex = new Dex894();
        dex.setNumberOfTransactions(3);
        dex.setTransactions(this.generateTransactions(3));
        dex.getTransactions().get(0).setSupplierNumber("INVOICE-A");
        dex.getTransactions().get(1).setSupplierNumber(null);
        dex.getTransactions().get(2).setSupplierNumber("INVOICE-A");

        Set<X12ErrorDetail> errorSet = dexValidator.validate(dex);
        assertNotNull(errorSet);
        assertEquals(2, errorSet.size());

        List<X12ErrorDetail> list = errorSet.stream()
            .sorted((o1,o2) -> o1.getMessage().compareTo(o2.getMessage()))
            .collect(Collectors.toList());

        X12ErrorDetail xed = list.get(0);
        assertEquals("G82", xed.getSegmentId());
        assertEquals("G8202", xed.getElementId());
        assertEquals("duplicate invoice numbers on DEX", xed.getMessage());

        xed = list.get(1);
        assertEquals("G82", xed.getSegmentId());
        assertEquals("G8202", xed.getElementId());
        assertEquals("missing supplier number", xed.getMessage());
    }

    @Test
    public void test_validate_transactionControlNumbersMismatched() {
        Dex894 dex = new Dex894();
        dex.setNumberOfTransactions(5);
        dex.setTransactions(this.generateTransactions(5));

        Dex894TransactionSet dexTx = dex.getTransactions().get(2);
        dexTx.setHeaderControlNumber("1234");
        dexTx.setTrailerControlNumber("4321");

        Set<X12ErrorDetail> errorSet = dexValidator.validate(dex);
        assertNotNull(errorSet);
        assertEquals(1, errorSet.size());
        assertEquals("SE", errorSet.stream().findFirst().get().getSegmentId());
    }

    @Test
    public void test_compareTransactionSegmentCounts() {
        Dex894TransactionSet dexTx = this.generateTransactions(1).get(0);

        X12ErrorDetail ed = dexValidator.compareTransactionSegmentCounts(4010, dexTx);
        assertNull(ed);
    }

    @Test
    public void test_compareTransactionSegmentCounts_expected_lessThan_Actual() {
        Dex894TransactionSet dexTx = this.generateTransactions(1).get(0);
        dexTx.setExpectedNumberOfSegments(5);
        dexTx.setActualNumberOfSegments(10);
        X12ErrorDetail ed = dexValidator.compareTransactionSegmentCounts(4010, dexTx);

        assertNotNull(ed);
        assertEquals("SE", ed.getSegmentId());
    }

    @Test
    public void test_compareTransactionSegmentCounts_expected_moreThan_Actual() {
        Dex894TransactionSet dexTx = this.generateTransactions(1).get(0);
        dexTx.setExpectedNumberOfSegments(10);
        dexTx.setActualNumberOfSegments(5);
        X12ErrorDetail ed = dexValidator.compareTransactionSegmentCounts(4010, dexTx);

        assertNotNull(ed);
        assertEquals("SE", ed.getSegmentId());
    }

    @Test
    public void test_compareTransactionSegmentCounts_zero() {
        Dex894TransactionSet dexTx = this.generateTransactions(1).get(0);
        dexTx.setExpectedNumberOfSegments(0);
        dexTx.setActualNumberOfSegments(0);
        X12ErrorDetail ed = dexValidator.compareTransactionSegmentCounts(4010, dexTx);

        assertNull(ed);
    }

    @Test
    public void test_compareTransactionSegmentCounts_null() {
        Dex894TransactionSet dexTx = this.generateTransactions(1).get(0);
        dexTx.setExpectedNumberOfSegments(null);
        dexTx.setActualNumberOfSegments(null);
        X12ErrorDetail ed = dexValidator.compareTransactionSegmentCounts(4010, dexTx);

        assertNull(ed);
    }

    @Test
    public void test_compareTransactionCounts() {
        Dex894 dex = new Dex894();
        dex.setNumberOfTransactions(5);
        dex.setTransactions(this.generateTransactions(5));

        X12ErrorDetail ed = dexValidator.compareTransactionCounts(dex);
        assertNull(ed);
    }

    @Test
    public void test_compareTransactionCounts_expected_lessThan_Actual() {
        Dex894 dex = new Dex894();
        dex.setNumberOfTransactions(2);
        dex.setTransactions(this.generateTransactions(5));

        X12ErrorDetail ed = dexValidator.compareTransactionCounts(dex);
        assertNotNull(ed);
        assertEquals("DXE", ed.getSegmentId());
    }

    @Test
    public void test_compareTransactionCounts_expected_moreThan_Actual() {
        Dex894 dex = new Dex894();
        dex.setNumberOfTransactions(12);
        dex.setTransactions(this.generateTransactions(5));

        X12ErrorDetail ed = dexValidator.compareTransactionCounts(dex);
        assertNotNull(ed);
        assertEquals("DXE", ed.getSegmentId());
    }

    @Test
    public void test_compareTransactionCounts_zero() {
        Dex894 dex = new Dex894();
        dex.setNumberOfTransactions(0);
        dex.setTransactions(this.generateTransactions(0));

        X12ErrorDetail ed = dexValidator.compareTransactionCounts(dex);
        assertNull(ed);
    }

    @Test
    public void test_compareTransactionCounts_noList() {
        Dex894 dex = new Dex894();
        dex.setNumberOfTransactions(0);
        dex.setTransactions(null);

        X12ErrorDetail ed = dexValidator.compareTransactionCounts(dex);
        assertNull(ed);
    }

    @Test
    public void test_checkForDuplicateInvoiceNumbers() {
        Dex894 dex = new Dex894();
        dex.setNumberOfTransactions(3);
        dex.setTransactions(this.generateTransactions(3));

        X12ErrorDetail ed = dexValidator.checkForDuplicateInvoiceNumbers(dex);
        assertNull(ed);
    }

    @Test
    public void test_checkForDuplicateInvoiceNumbersWithDuplicate() {
        Dex894 dex = new Dex894();
        dex.setNumberOfTransactions(5);
        dex.setTransactions(this.generateTransactions(5));

        // create dupe scenario
        String supplierNumber = dex.getTransactions().get(4).getSupplierNumber();
        dex.getTransactions().get(2).setSupplierNumber(supplierNumber);

        X12ErrorDetail ed = dexValidator.checkForDuplicateInvoiceNumbers(dex);
        assertNotNull(ed);
        assertEquals("G82", ed.getSegmentId());
        assertEquals("duplicate invoice numbers on DEX", ed.getMessage());
    }

    @Test
    public void test_checkForDuplicateInvoiceNumbers_noList() {
        Dex894 dex = new Dex894();
        dex.setNumberOfTransactions(0);
        dex.setTransactions(null);

        X12ErrorDetail ed = dexValidator.checkForDuplicateInvoiceNumbers(dex);
        assertNull(ed);
    }

    @Test
    public void test_validateItems_null() {
        Dex894TransactionSet dexTx = null;

        Set<X12ErrorDetail> errors = dexValidator.validateItems(4010, dexTx);
        assertNotNull(errors);
        assertEquals(0, errors.size());
    }

    @Test
    public void test_validateTransactions_4010() {
        Dex894TransactionSet dexTx = this.generateOneTransaction("INVOICE-A");
        dexTx.addItem(this.generateOneItem("1", UnitMeasure.EA));

        Set<X12ErrorDetail> errors = dexValidator.validateDexTransaction(4010, dexTx);
        assertNotNull(errors);
        assertEquals(0, errors.size());
    }

    @Test
    public void test_validateTransactions_5010() {
        Dex894TransactionSet dexTx = this.generateOneTransaction("INVOICE-A");
        dexTx.addItem(this.generateOneItem("1", UnitMeasure.EA, 5010));

        Set<X12ErrorDetail> errors = dexValidator.validateDexTransaction(5010, dexTx);
        assertNotNull(errors);
        assertEquals(0, errors.size());
    }

    @Test
    public void test_validateTransactions_null_supplier_number_4010() {
        Dex894TransactionSet dexTx = this.generateOneTransaction("INVOICE-A");
        dexTx.setSupplierNumber(null);
        dexTx.addItem(this.generateOneItem("1", UnitMeasure.EA));

        Set<X12ErrorDetail> errors = dexValidator.validateDexTransaction(4010, dexTx);
        assertNotNull(errors);
        assertEquals(1, errors.size());
        X12ErrorDetail xed = errors.stream().findFirst().get();
        assertEquals("G82", xed.getSegmentId());
        assertEquals("G8202", xed.getElementId());
        assertEquals("missing supplier number", xed.getMessage());
    }

    @Test
    public void test_validateTransactions_null_supplier_number_5010() {
        Dex894TransactionSet dexTx = this.generateOneTransaction("INVOICE-A");
        dexTx.setSupplierNumber(null);
        dexTx.addItem(this.generateOneItem("1", UnitMeasure.EA, 5010));

        Set<X12ErrorDetail> errors = dexValidator.validateDexTransaction(5010, dexTx);
        assertNotNull(errors);
        assertEquals(1, errors.size());
        X12ErrorDetail xed = errors.stream().findFirst().get();
        assertEquals("G82", xed.getSegmentId());
        assertEquals("G8202", xed.getElementId());
        assertEquals("missing supplier number", xed.getMessage());
    }

    @Test
    public void test_validateTransactions_missing_supplier_number_4010() {
        Dex894TransactionSet dexTx = this.generateOneTransaction("INVOICE-A");
        dexTx.setSupplierNumber("");
        dexTx.addItem(this.generateOneItem("1", UnitMeasure.EA));

        Set<X12ErrorDetail> errors = dexValidator.validateDexTransaction(4010, dexTx);
        assertNotNull(errors);
        assertEquals(1, errors.size());
        X12ErrorDetail xed = errors.stream().findFirst().get();
        assertEquals("G82", xed.getSegmentId());
        assertEquals("G8202", xed.getElementId());
        assertEquals("missing supplier number", xed.getMessage());
    }

    @Test
    public void test_validateTransactions_missing_supplier_number_5010() {
        Dex894TransactionSet dexTx = this.generateOneTransaction("INVOICE-A");
        dexTx.setSupplierNumber("");
        dexTx.addItem(this.generateOneItem("1", UnitMeasure.EA, 5010));

        Set<X12ErrorDetail> errors = dexValidator.validateDexTransaction(5010, dexTx);
        assertNotNull(errors);
        assertEquals(1, errors.size());
        X12ErrorDetail xed = errors.stream().findFirst().get();
        assertEquals("G82", xed.getSegmentId());
        assertEquals("G8202", xed.getElementId());
        assertEquals("missing supplier number", xed.getMessage());
    }

    @Test
    public void test_validateTransactions_null_supplier_date_4010() {
        Dex894TransactionSet dexTx = this.generateOneTransaction("INVOICE-A");
        dexTx.setTransactionDate(null);
        dexTx.addItem(this.generateOneItem("1", UnitMeasure.EA));

        Set<X12ErrorDetail> errors = dexValidator.validateDexTransaction(4010, dexTx);
        assertNotNull(errors);
        assertEquals(1, errors.size());
        X12ErrorDetail xed = errors.stream().findFirst().get();
        assertEquals("G82", xed.getSegmentId());
        assertEquals("G8207", xed.getElementId());
        assertEquals("missing supplier date", xed.getMessage());
    }

    @Test
    public void test_validateTransactions_null_supplier_date_5010() {
        Dex894TransactionSet dexTx = this.generateOneTransaction("INVOICE-A");
        dexTx.setTransactionDate(null);
        dexTx.addItem(this.generateOneItem("1", UnitMeasure.EA, 5010));

        Set<X12ErrorDetail> errors = dexValidator.validateDexTransaction(5010, dexTx);
        assertNotNull(errors);
        assertEquals(1, errors.size());
        X12ErrorDetail xed = errors.stream().findFirst().get();
        assertEquals("G82", xed.getSegmentId());
        assertEquals("G8207", xed.getElementId());
        assertEquals("missing supplier date", xed.getMessage());
    }

    @Test
    public void test_validateTransactions_missing_supplier_date_4010() {
        Dex894TransactionSet dexTx = this.generateOneTransaction("INVOICE-A");
        dexTx.setTransactionDate("");
        dexTx.addItem(this.generateOneItem("1", UnitMeasure.EA));

        Set<X12ErrorDetail> errors = dexValidator.validateDexTransaction(4010, dexTx);
        assertNotNull(errors);
        assertEquals(1, errors.size());
        X12ErrorDetail xed = errors.stream().findFirst().get();
        assertEquals("G82", xed.getSegmentId());
        assertEquals("G8207", xed.getElementId());
        assertEquals("missing supplier date", xed.getMessage());
    }

    @Test
    public void test_validateTransactions_missing_supplier_date_5010() {
        Dex894TransactionSet dexTx = this.generateOneTransaction("INVOICE-A");
        dexTx.setTransactionDate("");
        dexTx.addItem(this.generateOneItem("1", UnitMeasure.EA, 5010));

        Set<X12ErrorDetail> errors = dexValidator.validateDexTransaction(5010, dexTx);
        assertNotNull(errors);
        assertEquals(1, errors.size());
        X12ErrorDetail xed = errors.stream().findFirst().get();
        assertEquals("G82", xed.getSegmentId());
        assertEquals("G8207", xed.getElementId());
        assertEquals("missing supplier date", xed.getMessage());
    }

    @Test
    public void test_validateItems_4010() {
        Dex894TransactionSet dexTx = this.generateOneTransaction("INVOICE-A");
        dexTx.addItem(this.generateOneItem("1", UnitMeasure.EA));

        Set<X12ErrorDetail> errors = dexValidator.validateItems(4010, dexTx);
        assertNotNull(errors);
        assertEquals(0, errors.size());
    }

    @Test
    public void test_validateItems_5010() {
        Dex894TransactionSet dexTx = this.generateOneTransaction("INVOICE-A");
        dexTx.addItem(this.generateOneItem("1", UnitMeasure.EA, 5010));

        Set<X12ErrorDetail> errors = dexValidator.validateItems(5010, dexTx);
        assertNotNull(errors);
        assertEquals(0, errors.size());
    }

    @Test
    public void test_validateItems_missing_quantity_4010() {
        Dex894TransactionSet dexTx = this.generateOneTransaction("INVOICE-A");
        Dex894Item dexItem = this.generateOneItem("1", UnitMeasure.EA);
        dexItem.setQuantity(null);
        dexTx.addItem(dexItem);

        Set<X12ErrorDetail> errors = dexValidator.validateItems(4010, dexTx);
        assertNotNull(errors);
        assertEquals(1, errors.size());
        X12ErrorDetail xed = errors.stream().findFirst().get();
        assertEquals("G83", xed.getSegmentId());
        assertEquals("G8302", xed.getElementId());
        assertEquals("missing quantity", xed.getMessage());
    }

    @Test
    public void test_validateItems_missing_quantity_5010() {
        Dex894TransactionSet dexTx = this.generateOneTransaction("INVOICE-A");
        Dex894Item dexItem = this.generateOneItem("1", UnitMeasure.EA, 5010);
        dexItem.setQuantity(null);
        dexTx.addItem(dexItem);

        Set<X12ErrorDetail> errors = dexValidator.validateItems(5010, dexTx);
        assertNotNull(errors);
        assertEquals(1, errors.size());
        X12ErrorDetail xed = errors.stream().findFirst().get();
        assertEquals("G83", xed.getSegmentId());
        assertEquals("G8302", xed.getElementId());
        assertEquals("missing quantity", xed.getMessage());
    }

    @Test
    public void test_validateItems_negative_quantity_4010() {
        Dex894TransactionSet dexTx = this.generateOneTransaction("INVOICE-A");
        Dex894Item dexItem = this.generateOneItem("1", UnitMeasure.EA);
        dexItem.setQuantity(dexItem.getQuantity().negate());
        dexTx.addItem(dexItem);

        Set<X12ErrorDetail> errors = dexValidator.validateItems(4010, dexTx);
        assertNotNull(errors);
        assertEquals(1, errors.size());
        X12ErrorDetail xed = errors.stream().findFirst().get();
        assertEquals("G83", xed.getSegmentId());
        assertEquals("G8302", xed.getElementId());
        assertEquals("quantity must be positive", xed.getMessage());
    }

    @Test
    public void test_validateItems_negative_quantity_5010() {
        Dex894TransactionSet dexTx = this.generateOneTransaction("INVOICE-A");
        Dex894Item dexItem = this.generateOneItem("1", UnitMeasure.EA, 5010);
        dexItem.setQuantity(dexItem.getQuantity().negate());
        dexTx.addItem(dexItem);

        Set<X12ErrorDetail> errors = dexValidator.validateItems(5010, dexTx);
        assertNotNull(errors);
        assertEquals(1, errors.size());
        X12ErrorDetail xed = errors.stream().findFirst().get();
        assertEquals("G83", xed.getSegmentId());
        assertEquals("G8302", xed.getElementId());
        assertEquals("quantity must be positive", xed.getMessage());
    }


    @Test
    public void test_validateItems_missing_uom_4010() {
        Dex894TransactionSet dexTx = this.generateOneTransaction("INVOICE-A");
        Dex894Item dexItem = this.generateOneItem("1", UnitMeasure.EA);
        dexItem.setUom(null);
        dexTx.addItem(dexItem);

        Set<X12ErrorDetail> errors = dexValidator.validateItems(4010, dexTx);
        assertNotNull(errors);
        assertEquals(1, errors.size());
        X12ErrorDetail xed = errors.stream().findFirst().get();
        assertEquals("G83", xed.getSegmentId());
        assertEquals("G8303", xed.getElementId());
        assertEquals("missing/unknown unit of measure", xed.getMessage());
    }

    @Test
    public void test_validateItems_missing_uom_5010() {
        Dex894TransactionSet dexTx = this.generateOneTransaction("INVOICE-A");
        Dex894Item dexItem = this.generateOneItem("1", UnitMeasure.EA, 5010);
        dexItem.setUom(null);
        dexTx.addItem(dexItem);

        Set<X12ErrorDetail> errors = dexValidator.validateItems(5010, dexTx);
        assertNotNull(errors);
        assertEquals(1, errors.size());
        X12ErrorDetail xed = errors.stream().findFirst().get();
        assertEquals("G83", xed.getSegmentId());
        assertEquals("G8303", xed.getElementId());
        assertEquals("missing/unknown unit of measure", xed.getMessage());
    }

    @Test
    public void test_validateItems_unknown_uom_4010() {
        Dex894TransactionSet dexTx = this.generateOneTransaction("INVOICE-A");
        Dex894Item dexItem = this.generateOneItem("1", UnitMeasure.EA);
        dexItem.setUom(UnitMeasure.UNKNOWN);
        dexTx.addItem(dexItem);

        Set<X12ErrorDetail> errors = dexValidator.validateItems(4010, dexTx);
        assertNotNull(errors);
        assertEquals(1, errors.size());
        X12ErrorDetail xed = errors.stream().findFirst().get();
        assertEquals("G83", xed.getSegmentId());
        assertEquals("G8303", xed.getElementId());
        assertEquals("missing/unknown unit of measure", xed.getMessage());
    }

    @Test
    public void test_validateItems_unknown_uom_5010() {
        Dex894TransactionSet dexTx = this.generateOneTransaction("INVOICE-A");
        Dex894Item dexItem = this.generateOneItem("1", UnitMeasure.EA, 5010);
        dexItem.setUom(UnitMeasure.UNKNOWN);
        dexTx.addItem(dexItem);

        Set<X12ErrorDetail> errors = dexValidator.validateItems(5010, dexTx);
        assertNotNull(errors);
        assertEquals(1, errors.size());
        X12ErrorDetail xed = errors.stream().findFirst().get();
        assertEquals("G83", xed.getSegmentId());
        assertEquals("G8303", xed.getElementId());
        assertEquals("missing/unknown unit of measure", xed.getMessage());
    }


    @Test
    public void test_validateItems_null_upc_4010() {
        Dex894TransactionSet dexTx = this.generateOneTransaction("INVOICE-A");
        Dex894Item dexItem = this.generateOneItem("1", UnitMeasure.EA);
        dexItem.setUpc(null);
        dexTx.addItem(dexItem);

        Set<X12ErrorDetail> errors = dexValidator.validateItems(4010, dexTx);
        assertNotNull(errors);
        assertEquals(1, errors.size());
        X12ErrorDetail xed = errors.stream().findFirst().get();
        assertEquals("G83", xed.getSegmentId());
        assertEquals("G8304", xed.getElementId());
        assertEquals("missing consumer UPC", xed.getMessage());
    }

    @Test
    public void test_validateItems_empty_upc_4010() {
        Dex894TransactionSet dexTx = this.generateOneTransaction("INVOICE-A");
        Dex894Item dexItem = this.generateOneItem("1", UnitMeasure.EA);
        dexItem.setUpc("");
        dexTx.addItem(dexItem);

        Set<X12ErrorDetail> errors = dexValidator.validateItems(4010, dexTx);
        assertNotNull(errors);
        assertEquals(1, errors.size());
        X12ErrorDetail xed = errors.stream().findFirst().get();
        assertEquals("G83", xed.getSegmentId());
        assertEquals("G8304", xed.getElementId());
        assertEquals("missing consumer UPC", xed.getMessage());
    }

    @Test
    public void test_validateItems_null_product_id_5010() {
        Dex894TransactionSet dexTx = this.generateOneTransaction("INVOICE-A");
        Dex894Item dexItem = this.generateOneItem("1", UnitMeasure.EA, 5010);
        dexItem.setConsumerProductId(null);
        dexTx.addItem(dexItem);

        Set<X12ErrorDetail> errors = dexValidator.validateItems(5010, dexTx);
        assertNotNull(errors);
        assertEquals(1, errors.size());
        X12ErrorDetail xed = errors.stream().findFirst().get();
        assertEquals("G83", xed.getSegmentId());
        assertEquals("G8306", xed.getElementId());
        assertEquals("missing consumer UPC", xed.getMessage());
    }

    @Test
    public void test_validateItems_empty_product_id_5010() {
        Dex894TransactionSet dexTx = this.generateOneTransaction("INVOICE-A");
        Dex894Item dexItem = this.generateOneItem("1", UnitMeasure.EA, 5010);
        dexItem.setConsumerProductId("");
        dexTx.addItem(dexItem);

        Set<X12ErrorDetail> errors = dexValidator.validateItems(5010, dexTx);
        assertNotNull(errors);
        assertEquals(1, errors.size());
        X12ErrorDetail xed = errors.stream().findFirst().get();
        assertEquals("G83", xed.getSegmentId());
        assertEquals("G8306", xed.getElementId());
        assertEquals("missing consumer UPC", xed.getMessage());
    }

    @Test
    public void test_validateItems_missing_product_type_5010() {
        Dex894TransactionSet dexTx = this.generateOneTransaction("INVOICE-A");
        Dex894Item dexItem = this.generateOneItem("1", UnitMeasure.EA, 5010);
        dexItem.setConsumerProductQualifier(null);
        dexTx.addItem(dexItem);

        Set<X12ErrorDetail> errors = dexValidator.validateItems(5010, dexTx);
        assertNotNull(errors);
        assertEquals(1, errors.size());
        X12ErrorDetail xed = errors.stream().findFirst().get();
        assertEquals("G83", xed.getSegmentId());
        assertEquals("G8305", xed.getElementId());
        assertEquals("missing consumer qualifier", xed.getMessage());
    }

    @Test
    public void test_validateItems_with_case_4010() {
        Dex894TransactionSet dexTx = this.generateOneTransaction("INVOICE-A");
        // case of an item
        Dex894Item dexItem = this.generateOneItem("1", UnitMeasure.CA);
        dexTx.addItem(dexItem);
        // eaches of an item
        dexTx.addItem(this.generateOneItem("2", UnitMeasure.EA));

        Set<X12ErrorDetail> errors = dexValidator.validateItems(4010, dexTx);
        assertNotNull(errors);
        assertEquals(0, errors.size());
    }

    @Test
    public void test_validateItems_with_case_upc_missing_4010() {
        Dex894TransactionSet dexTx = this.generateOneTransaction("INVOICE-A");
        // case of an item
        Dex894Item dexItem = this.generateOneItem("1", UnitMeasure.CA);
        dexItem.setCaseUpc(null);
        dexTx.addItem(dexItem);
        // eaches of an item
        dexTx.addItem(this.generateOneItem("2", UnitMeasure.EA));

        Set<X12ErrorDetail> errors = dexValidator.validateItems(4010, dexTx);
        assertNotNull(errors);
        assertEquals(1, errors.size());
        X12ErrorDetail xed = errors.stream().findFirst().get();
        assertEquals("G83", xed.getSegmentId());
        assertEquals("G8307", xed.getElementId());
        assertEquals("missing case UPC", xed.getMessage());
    }

    @Test
    public void test_validateItems_with_case_upc_missing_pack_count_4010() {
        Dex894TransactionSet dexTx = this.generateOneTransaction("INVOICE-A");
        // case of an item
        Dex894Item dexItem = this.generateOneItem("1", UnitMeasure.CA);
        dexItem.setPackCount(null);
        dexTx.addItem(dexItem);
        // eaches of an item
        dexTx.addItem(this.generateOneItem("2", UnitMeasure.EA));

        Set<X12ErrorDetail> errors = dexValidator.validateItems(4010, dexTx);
        assertNotNull(errors);
        assertEquals(1, errors.size());
        X12ErrorDetail xed = errors.stream().findFirst().get();
        assertEquals("G83", xed.getSegmentId());
        assertEquals("G8309", xed.getElementId());
        assertEquals("missing case count", xed.getMessage());
    }

    @Test
    public void test_validateItems_with_case_5010() {
        Dex894TransactionSet dexTx = this.generateOneTransaction("INVOICE-A");
        // case of an item
        Dex894Item dexItem = this.generateOneItem("1", UnitMeasure.CA, 5010);
        dexItem.setCaseUpc(null);
        dexTx.addItem(dexItem);
        // eaches of an item
        dexTx.addItem(this.generateOneItem("2", UnitMeasure.EA, 5010));

        Set<X12ErrorDetail> errors = dexValidator.validateItems(5010, dexTx);
        assertNotNull(errors);
        assertEquals(0, errors.size());
    }

    @Test
    public void test_validateItems_with_case_upc_missing_5010() {
        Dex894TransactionSet dexTx = this.generateOneTransaction("INVOICE-A");
        // case of an item
        Dex894Item dexItem = this.generateOneItem("1", UnitMeasure.CA, 5010);
        dexItem.setUom(UnitMeasure.CA);
        dexItem.setCaseUpc(null);
        dexItem.setCaseProductQualifier(ProductQualifier.UK);
        dexItem.setCaseProductId(null);
        dexItem.setPackCount(10);
        dexTx.addItem(dexItem);
        // eaches of an item
        dexTx.addItem(this.generateOneItem("2", UnitMeasure.EA, 5010));

        Set<X12ErrorDetail> errors = dexValidator.validateItems(5010, dexTx);
        assertNotNull(errors);
        assertEquals(1, errors.size());
        X12ErrorDetail xed = errors.stream().findFirst().get();
        assertEquals("G83", xed.getSegmentId());
        assertEquals("G8312", xed.getElementId());
        assertEquals("missing case UPC", xed.getMessage());
    }

    @Test
    public void test_validateItems_with_case_upc_qualifier_missing_5010() {
        Dex894TransactionSet dexTx = this.generateOneTransaction("INVOICE-A");
        // case of an item
        Dex894Item dexItem = this.generateOneItem("1", UnitMeasure.CA, 5010);
        dexItem.setUom(UnitMeasure.CA);
        dexItem.setCaseUpc(null);
        dexItem.setCaseProductQualifier(null);
        dexItem.setCaseProductId("00014100085478");
        dexItem.setPackCount(10);
        dexTx.addItem(dexItem);
        // eaches of an item
        dexTx.addItem(this.generateOneItem("2", UnitMeasure.EA, 5010));

        Set<X12ErrorDetail> errors = dexValidator.validateItems(5010, dexTx);
        assertNotNull(errors);
        assertEquals(1, errors.size());
        X12ErrorDetail xed = errors.stream().findFirst().get();
        assertEquals("G83", xed.getSegmentId());
        assertEquals("G8311", xed.getElementId());
        assertEquals("missing case qualifier", xed.getMessage());
    }

    @Test
    public void test_validateItems_with_case_upc_missing_pack_count_5010() {
        Dex894TransactionSet dexTx = this.generateOneTransaction("INVOICE-A");
        // case of an item
        Dex894Item dexItem = this.generateOneItem("1", UnitMeasure.CA, 5010);
        dexItem.setUom(UnitMeasure.CA);
        dexItem.setCaseUpc(null);
        dexItem.setCaseProductQualifier(ProductQualifier.UK);
        dexItem.setCaseProductId("00014100085478");
        dexItem.setPackCount(null);
        dexTx.addItem(dexItem);
        // eaches of an item
        dexTx.addItem(this.generateOneItem("2", UnitMeasure.EA, 5010));

        Set<X12ErrorDetail> errors = dexValidator.validateItems(5010, dexTx);
        assertNotNull(errors);
        assertEquals(1, errors.size());
        X12ErrorDetail xed = errors.stream().findFirst().get();
        assertEquals("G83", xed.getSegmentId());
        assertEquals("G8309", xed.getElementId());
        assertEquals("missing case count", xed.getMessage());
    }

    @Test
    public void test_validateItems_with_case_upc_missing_pack_count_5010_with_4010_data() {
        Dex894TransactionSet dexTx = this.generateOneTransaction("INVOICE-A");
        // case of an item
        Dex894Item dexItem = this.generateOneItem("1", UnitMeasure.CA, 4010);
        dexTx.addItem(dexItem);
        // eaches of an item
        dexTx.addItem(this.generateOneItem("2", UnitMeasure.EA, 4010));

        // validate a 4010 data set w/ 5010
        Set<X12ErrorDetail> errors = dexValidator.validateItems(5010, dexTx);
        assertNotNull(errors);
        assertEquals(2, errors.size());
        List<X12ErrorDetail> list = errors.stream()
            .sorted((o1,o2) -> o1.getElementId().compareTo(o2.getElementId()))
            .collect(Collectors.toList());
        X12ErrorDetail xed = list.get(0);
        assertEquals("G83", xed.getSegmentId());
        assertEquals("G8305", xed.getElementId());
        assertEquals("missing consumer qualifier", xed.getMessage());

        xed = list.get(1);
        assertEquals("G83", xed.getSegmentId());
        assertEquals("G8311", xed.getElementId());
        assertEquals("missing case qualifier", xed.getMessage());
    }


    protected List<Dex894TransactionSet> generateTransactions(int numTx) {
        List<Dex894TransactionSet> dexTxList = new ArrayList<>();

        for (int i = 0; i < numTx; i++) {
            dexTxList.add(this.generateOneTransaction("invoice-" + i));
        }

        return dexTxList;
    }

    protected Dex894TransactionSet generateOneTransaction(String invoiceNumber) {
        Dex894TransactionSet dexTx = new Dex894TransactionSet();
        dexTx.setHeaderControlNumber("569145631");
        dexTx.setTrailerControlNumber("569145631");
        dexTx.setSupplierNumber(invoiceNumber);
        dexTx.setTransactionDate("19770525");
        dexTx.setExpectedNumberOfSegments(10);
        dexTx.setActualNumberOfSegments(10);

        return dexTx;
    }

    protected Dex894Item generateOneItem(String seqNumber, UnitMeasure uom) {
        return this.generateOneItem(seqNumber, uom, 4010);
    }

    protected Dex894Item generateOneItem(String seqNumber, UnitMeasure uom, int dexVersion) {
        Dex894Item dexItem = new Dex894Item();
        dexItem.setItemSequenceNumber(seqNumber);
        dexItem.setQuantity(new BigDecimal(5));
        dexItem.setUom(uom);
        if (dexVersion <= 4010) {
            dexItem.setUpc("492130600210");
            if (UnitMeasure.CA.equals(uom)) {
                dexItem.setCaseUpc("492130600210");
                dexItem.setPackCount(4);
            }
        } else {
            dexItem.setConsumerProductQualifier(ProductQualifier.UP);
            dexItem.setConsumerProductId("492130600210");
            if (UnitMeasure.CA.equals(uom)) {
                dexItem.setCaseProductQualifier(ProductQualifier.UP);
                dexItem.setCaseProductId("492130600210");
                dexItem.setPackCount(4);
            }
        }
        dexItem.setItemDescription("Guinness Extra Stout 12 oz, 6 pk");

        return dexItem;
    }
}
