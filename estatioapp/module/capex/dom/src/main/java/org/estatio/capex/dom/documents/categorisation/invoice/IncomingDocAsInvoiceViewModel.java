/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.estatio.capex.dom.documents.categorisation.invoice;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;

import org.joda.time.LocalDate;

import org.apache.isis.applib.annotation.Action;
import org.apache.isis.applib.annotation.DomainObject;
import org.apache.isis.applib.annotation.Editing;
import org.apache.isis.applib.annotation.MinLength;
import org.apache.isis.applib.annotation.Optionality;
import org.apache.isis.applib.annotation.Parameter;
import org.apache.isis.applib.annotation.Programmatic;
import org.apache.isis.applib.annotation.Property;
import org.apache.isis.applib.annotation.SemanticsOf;
import org.apache.isis.applib.annotation.Where;
import org.apache.isis.applib.services.clock.ClockService;
import org.apache.isis.applib.services.factory.FactoryService;
import org.apache.isis.schema.utils.jaxbadapters.JodaLocalDateStringAdapter;

import org.incode.module.document.dom.impl.docs.Document;

import org.estatio.capex.dom.documents.categorisation.document.IncomingDocViewModel;
import org.estatio.capex.dom.invoice.IncomingInvoice;
import org.estatio.capex.dom.order.Order;
import org.estatio.capex.dom.order.OrderItem;
import org.estatio.capex.dom.order.OrderItemRepository;
import org.estatio.capex.dom.order.OrderRepository;
import org.estatio.dom.financial.bankaccount.BankAccount;
import org.estatio.dom.invoice.PaymentMethod;
import org.estatio.dom.party.Party;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@DomainObject(
        // WORKAROUND: using fqcn as objectType because Isis' invalidation of cache in prototyping mode causing NPEs in some situations
        objectType = "org.estatio.capex.dom.documents.categorisation.invoice.IncomingDocAsInvoiceViewModel"
)
@XmlRootElement(name = "categorizeIncomingInvoice")
@XmlType(
        propOrder = {
                "document",
                "invoiceNumber",
                "buyer",
                "seller",
                "bankAccount",
                "dateReceived",
                "invoiceDate",
                "dueDate",
                "paymentMethod",
                "description",
                "orderItem",
                "fixedAsset",
                "project",
                "period",
                "budgetItem",
                "charge",
                "netAmount",
                "vatAmount",
                "tax",
                "grossAmount",
                "notCorrect",
                "domainObject",
                "originatingTask"
        }
)
@XmlAccessorType(XmlAccessType.FIELD)
@Getter @Setter
public class IncomingDocAsInvoiceViewModel
        extends IncomingDocViewModel<IncomingInvoice>
        implements SellerBankAccountCreator  {

    /**
     * for unit testing
     */
    IncomingDocAsInvoiceViewModel() {
    }

    public IncomingDocAsInvoiceViewModel(final Document document) {
        super(document);
        setDateReceived(document.getCreatedAt().toLocalDate());
        setDueDate(document.getCreatedAt().toLocalDate().plusDays(30));

        // TODO: this will need to default to BANK_TRANSFER when we get to live...
        setPaymentMethod(PaymentMethod.TEST_NO_PAYMENT);
    }

    /**
     * Populated once this view model is actioned; stored just so can be read by subscribers on any actions
     * for this view model that publish domain events.
     */
    @Property(hidden = Where.EVERYWHERE)
    IncomingInvoice domainObject;

    @org.apache.isis.applib.annotation.Property(editing = Editing.ENABLED)
    private BankAccount bankAccount;
    public void modifyBankAccount(final BankAccount bankAccount){
        setBankAccount(bankAccount);
        setSeller(bankAccount.getOwner());
    }

    public List<BankAccount> autoCompleteBankAccount(@MinLength(3) final String searchString){
        if (getSeller()!=null){
            return bankAccountRepository.findBankAccountsByOwner(getSeller());
        } else {
            return bankAccountRepository.autoComplete(searchString);
        }
    }

    //endregion


    @Property(editing = Editing.ENABLED)
    private String invoiceNumber;

    @XmlJavaTypeAdapter(JodaLocalDateStringAdapter.ForJaxb.class)
    @Property(editing = Editing.ENABLED)
    private LocalDate dateReceived;

    public LocalDate defaultDateReceived(){
        return getDateReceived()==null ? dateReceivedDerivedFromDocument() : getDateReceived();
    }

    private LocalDate dateReceivedDerivedFromDocument() {
        return getDocument().getCreatedAt().toLocalDate();
    }

    @XmlJavaTypeAdapter(JodaLocalDateStringAdapter.ForJaxb.class)
    @Property(editing = Editing.ENABLED)
    private LocalDate invoiceDate;

    public void modifyInvoiceDate(LocalDate invoiceDate){
        setInvoiceDate(invoiceDate);
        updateDueDate();
    }

    @XmlJavaTypeAdapter(JodaLocalDateStringAdapter.ForJaxb.class)
    @Property(editing = Editing.ENABLED)
    private LocalDate dueDate;

    @Property(editing = Editing.ENABLED)
    private PaymentMethod paymentMethod;

    @Property(editing = Editing.ENABLED)
    private Boolean notCorrect;

    public PaymentMethod defaultPaymentMethod(){
        return getPaymentMethod();
    }

    @Property(editing = Editing.ENABLED)
    private OrderItem orderItem;
    public void modifyOrderItem(OrderItem orderItem) {
        setOrderItem(orderItem);
        autoFillIn();
    }

    public List<OrderItem> autoCompleteOrderItem(@MinLength(3) final String searchString){
        List<OrderItem> result = new ArrayList<>();

        for (Order order : orderRepository.matchByOrderNumber(searchString)){
            for (OrderItem item : order.getItems()) {
                if (!result.contains(item)) {
                    result.add(item);
                }
            }
        }

        for (OrderItem item : orderItemRepository.matchByDescription(searchString)) {
            if (!result.contains(item)) {
                result.add(item);
            }
        }

        if (hasSeller()){
            result = Lists.newArrayList(
                    FluentIterable.from(result)
                    .filter(x->x.getOrdr().getSeller()!=null && x.getOrdr().getSeller().equals(getSeller()))
                    .toList()
            );
        }
        if (hasCharge()) {
            result = Lists.newArrayList(
                    FluentIterable.from(result)
                            .filter(x->x.getCharge().equals(getCharge()))
                            .toList()
            );
        }
        if (hasProject()) {
            result = Lists.newArrayList(
                    FluentIterable.from(result)
                            .filter(x->x.getProject()!=null && x.getProject().equals(getProject()))
                            .toList()
            );
        }
        if (hasFixedAsset()) {
            result = Lists.newArrayList(
                    FluentIterable.from(result)
                            .filter(x->x.getProperty()!=null && x.getProperty().equals(getFixedAsset()))
                            .toList()
            );
        }
        return result;
    }

    @Override
    public void modifySeller(final Party seller){
        setSeller(seller);
        setBankAccount(getFirstBankAccountOfPartyOrNull(seller));
    }


    @Action(
            semantics = SemanticsOf.IDEMPOTENT
    )
    public IncomingDocAsInvoiceViewModel changeInvoiceDetails(
            final String invoiceNumber,
            @Parameter(optionality = Optionality.OPTIONAL)
            final Party buyer,
            @Parameter(optionality = Optionality.OPTIONAL)
            final Party seller,
            final LocalDate dateReceived,
            @Parameter(optionality = Optionality.OPTIONAL)
            final LocalDate invoiceDate,
            @Parameter(optionality = Optionality.OPTIONAL)
            final LocalDate dueDate,
            @Parameter(optionality = Optionality.OPTIONAL)
            final Integer dueInNumberOfDaysFromNow,
            @Parameter(optionality = Optionality.OPTIONAL)
            final PaymentMethod paymentMethod
    ){
        setInvoiceNumber(invoiceNumber);
        setBuyer(buyer);
        setSeller(seller);
        setBankAccount(getFirstBankAccountOfPartyOrNull(seller));
        setDateReceived(dateReceived);
        setInvoiceDate(invoiceDate);
        setDueDate(dueDate);
        if (dueInNumberOfDaysFromNow!=null){
            setDueDate(clockService.now().plusDays(dueInNumberOfDaysFromNow));
        }
        setPaymentMethod(paymentMethod);
        return this;
    }

    public String default0ChangeInvoiceDetails(){
        return getInvoiceNumber();
    }

    public Party default1ChangeInvoiceDetails(){
        return getBuyer();
    }

    public Party default2ChangeInvoiceDetails(){
        return getSeller();
    }

    public LocalDate default3ChangeInvoiceDetails(){
        return getDateReceived()==null ? dateReceivedDerivedFromDocument() : getDateReceived();
    }

    public LocalDate default4ChangeInvoiceDetails(){
        return getInvoiceDate();
    }

    public LocalDate default5ChangeInvoiceDetails(){
        return getDueDate();
    }

    public PaymentMethod default7ChangeInvoiceDetails(){
        return getPaymentMethod()==null ? PaymentMethod.TEST_NO_PAYMENT : getPaymentMethod();
    }

    @Programmatic
    public void updateDueDate(){
        if (getInvoiceDate()!=null){
            setDueDate(getInvoiceDate().plusMonths(1));
        }
    }

    @Programmatic
    public void autoFillIn(){
        if (hasOrderItem()){
            Order order = orderRepository.findByOrderNumber(getOrderItem().getOrdr().getOrderNumber());
            OrderItem orderItem = orderItemRepository.findByOrderAndCharge(order, getOrderItem().getCharge());
            if (!(hasNetAmount() && hasGrossAmount() && hasVatAmount())){
                setNetAmount(orderItem.getNetAmount());
                setVatAmount(orderItem.getVatAmount());
                setGrossAmount(orderItem.getGrossAmount());
            }
            if (!hasTax()){
                setTax(orderItem.getTax());
            }
            if (!hasBuyer()){
                setBuyer(order.getBuyer());
            }
            if (!hasSeller()){
                setSeller(order.getSeller());
                setBankAccount(getFirstBankAccountOfPartyOrNull(order.getSeller()));
            }
            if (!hasDescription()){
                setDescription(orderItem.getDescription());
            }
            if (!hasCharge()){
                setCharge(orderItem.getCharge());
            }
            if (!hasProject()){
                setProject(orderItem.getProject());
            }
            if (!hasFixedAsset()){
                setFixedAsset(orderItem.getFixedAsset());
            }
            if (!hasBudgetItem()){
                setBudgetItem(orderItem.getBudgetItem());
            }
            if (!hasPeriod()){
                setPeriod(orderItem.getPeriod());
            }
        }
    }

    private BankAccount getFirstBankAccountOfPartyOrNull(final Party party){
        return bankAccountRepository.findBankAccountsByOwner(party).size()>0 ?
                bankAccountRepository.findBankAccountsByOwner(party).get(0) : null;
    }

    protected String minimalRequiredDataToComplete(){
        StringBuffer buffer = new StringBuffer();
        if (getInvoiceNumber()==null){
            buffer.append("invoice number, ");
        }
        if (getBuyer()==null){
            buffer.append("buyer, ");
        }
        if (getSeller()==null){
            buffer.append("seller, ");
        }
        if (getDateReceived()==null){
            buffer.append("date received, ");
        }
        if (getDueDate()==null){
            buffer.append("due date, ");
        }
        if (getPaymentMethod()==null){
            buffer.append("payment method, ");
        }
        if (getNetAmount()==null){
            buffer.append("net amount, ");
        }
        if (getGrossAmount()==null){
            buffer.append("gross amount, ");
        }
        if (getPeriod()==null){
            buffer.append("period, ");
        }
        if (buffer.length()==0){
            return null;
        } else {
            return buffer.replace(buffer.length()-2, buffer.length(), " required").toString();
        }
    }


    @Programmatic
    public boolean hasOrderItem(){
        return getOrderItem() != null;
    }

    @Inject
    @XmlTransient
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    OrderItemRepository orderItemRepository;

    @Inject
    @XmlTransient
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    OrderRepository orderRepository;

    @Inject
    @XmlTransient
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private ClockService clockService;

    @Inject
    @XmlTransient
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    FactoryService factoryService;
}
