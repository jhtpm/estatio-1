/*
 *
 *  Copyright 2012-2014 Eurocommercial Properties NV
 *
 *
 *  Licensed under the Apache License, Version 2.0 (the
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
package org.estatio.integtests.invoice;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import com.google.common.io.Resources;

import org.assertj.core.data.Percentage;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import org.apache.isis.applib.fixturescripts.FixtureScript;
import org.apache.isis.applib.services.bookmark.Bookmark;
import org.apache.isis.applib.services.bookmark.BookmarkService2;
import org.apache.isis.applib.services.clock.ClockService;
import org.apache.isis.applib.services.wrapper.HiddenException;
import org.apache.isis.applib.services.xactn.TransactionService;
import org.apache.isis.applib.value.Blob;

import org.incode.module.communications.dom.impl.commchannel.CommunicationChannel;
import org.incode.module.communications.dom.impl.commchannel.EmailAddress;
import org.incode.module.communications.dom.impl.commchannel.PostalAddress;
import org.incode.module.communications.dom.impl.comms.Communication;
import org.incode.module.communications.dom.impl.comms.CommunicationState;
import org.incode.module.document.dom.impl.docs.Document;
import org.incode.module.document.dom.impl.docs.DocumentAbstract;
import org.incode.module.document.dom.impl.docs.DocumentSort;
import org.incode.module.document.dom.impl.docs.DocumentState;
import org.incode.module.document.dom.impl.docs.DocumentTemplate;
import org.incode.module.document.dom.impl.docs.DocumentTemplateRepository;
import org.incode.module.document.dom.impl.docs.Document_delete;
import org.incode.module.document.dom.impl.paperclips.Paperclip;
import org.incode.module.document.dom.impl.paperclips.PaperclipRepository;
import org.incode.module.document.dom.impl.paperclips.Paperclip_changeRole;
import org.incode.module.document.dom.impl.types.DocumentType;
import org.incode.module.document.dom.impl.types.DocumentTypeRepository;

import org.estatio.dom.invoice.DocumentTypeData;
import org.estatio.dom.lease.invoicing.dnc.PaperclipRoleNames;
import org.estatio.dom.invoice.Invoice;
import org.estatio.dom.invoice.InvoiceRepository;
import org.estatio.dom.invoice.InvoiceStatus;
import org.estatio.dom.invoice.PaymentMethod;
import org.estatio.dom.lease.invoicing.dnc.Invoice_attachSupportingDocument;
import org.estatio.dom.lease.invoicing.dnc.Invoice_prepare;
import org.estatio.dom.lease.invoicing.dnc.Invoice_sendByEmail;
import org.estatio.dom.lease.invoicing.dnc.Invoice_sendByPost;
import org.estatio.dom.lease.invoicing.viewmodel.dnc.DocAndCommForInvoiceDoc;
import org.estatio.dom.lease.invoicing.viewmodel.dnc.DocAndCommForInvoiceDoc_communication;
import org.estatio.dom.lease.invoicing.viewmodel.dnc.DocAndCommForInvoiceDoc_communicationState;
import org.estatio.dom.lease.invoicing.viewmodel.dnc.DocAndCommForInvoiceDoc_document;
import org.estatio.dom.lease.invoicing.viewmodel.dnc.DocAndCommForInvoiceDoc_documentState;
import org.estatio.dom.lease.invoicing.viewmodel.dnc.DocAndCommForPrelimLetter;
import org.estatio.dom.lease.invoicing.viewmodel.dnc.DocAndCommForPrelimLetter_communication;
import org.estatio.dom.lease.invoicing.viewmodel.dnc.DocAndCommForPrelimLetter_communicationState;
import org.estatio.dom.lease.invoicing.viewmodel.dnc.DocAndCommForPrelimLetter_document;
import org.estatio.dom.lease.invoicing.viewmodel.dnc.DocAndCommForPrelimLetter_documentState;
import org.estatio.dom.lease.invoicing.viewmodel.dnc.Invoice_invoiceDocs;
import org.estatio.dom.lease.invoicing.viewmodel.dnc.Invoice_ForLease_preliminaryLetters;
import org.estatio.dom.lease.Lease;
import org.estatio.dom.lease.LeaseRepository;
import org.estatio.dom.lease.invoicing.InvoiceForLease;
import org.estatio.dom.lease.invoicing.InvoiceForLeaseRepository;
import org.estatio.dom.lease.invoicing.InvoiceForLease_overrideSendTo;
import org.estatio.dom.party.Party;
import org.estatio.dom.party.PartyRepository;
import org.estatio.fixture.EstatioBaseLineFixture;
import org.estatio.fixture.invoice.InvoiceForLeaseItemTypeOfRentOneQuarterForOxfPoison003;
import org.estatio.fixturescripts.DocumentTypesAndTemplatesFixture;
import org.estatio.integtests.EstatioIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

public class Invoice_DocumentManagement_IntegTest extends EstatioIntegrationTest {

    @Before
    public void setupData() {
        runFixtureScript(new FixtureScript() {
            @Override
            protected void execute(ExecutionContext executionContext) {
                executionContext.executeChild(this, new EstatioBaseLineFixture());
                executionContext.executeChild(this, new InvoiceForLeaseItemTypeOfRentOneQuarterForOxfPoison003());

                executionContext.executeChild(this, new DocumentTypesAndTemplatesFixture());
            }
        });
    }


    public static class Invoice_createAndAttachDocument_IntegTest extends Invoice_DocumentManagement_IntegTest {

        public static class ActionInvocationIntegTest extends
                Invoice_DocumentManagement_IntegTest {

            @Test
            public void for_prelim_letter() throws Exception {

                // given
                Invoice invoice = findInvoice(InvoiceStatus.NEW);
                DocumentTemplate prelimLetterTemplate = findDocumentTemplateFor(DocumentTypeData.PRELIM_LETTER, invoice);

                // when
                wrap(mixin(Invoice_prepare.class, invoice)).$$(prelimLetterTemplate);
                Document document = prelimLetterOf(invoice);

                // then
                assertThat(document).isNotNull();
                List<Paperclip> paperclips = paperclipRepository.findByDocument(document);
                assertThat(paperclips).hasSize(1);

                assertThat(paperclips).extracting(x -> x.getAttachedTo()).contains(invoice);
            }

            @Test
            public void for_invoice_doc() throws Exception {

                // given
                Invoice invoice = findInvoice(InvoiceStatus.NEW);
                DocumentTemplate invoiceDocTemplate = findDocumentTemplateFor(DocumentTypeData.INVOICE, invoice);

                // given the invoice has been invoiced
                approveAndInvoice(invoice);

                // when
                wrap(mixin(Invoice_prepare.class, invoice)).$$(invoiceDocTemplate);
                Document document = invoiceDocOf(invoice);

                // then
                assertThat(document).isNotNull();
                List<Paperclip> paperclips = paperclipRepository.findByDocument(document);
                assertThat(paperclips).hasSize(1);

                assertThat(paperclips).extracting(x -> x.getAttachedTo()).contains(invoice);
            }

            @Test
            public void cannot_create_invoice_doc_if_the_invoice_has_not_yet_been_invoiced() throws Exception {

                // given
                Invoice invoice = findInvoice(InvoiceStatus.NEW);
                DocumentTemplate prelimLetterTemplate = findDocumentTemplateFor(DocumentTypeData.PRELIM_LETTER, invoice);
                DocumentTemplate invoiceDocTemplate = findDocumentTemplateFor(DocumentTypeData.INVOICE, invoice);

                // when
                final List<DocumentTemplate> documentTemplates =
                        mixin(Invoice_prepare.class, invoice).choices0$$();

                // then
                assertThat(documentTemplates).doesNotContain(invoiceDocTemplate);
                assertThat(documentTemplates).contains(prelimLetterTemplate);
            }
        }

    }


    public static class Invoice_attachSupportingDocument_IntegTest extends Invoice_DocumentManagement_IntegTest {

        @Test
        public void attaches_to_invoice() throws Exception {

            // given
            Invoice invoice = findInvoice(InvoiceStatus.NEW);
            assertThat(invoice).isNotNull();
            assertThat(invoice.getStatus().invoiceIsChangable()).isTrue();

            List<Paperclip> paperclips = paperclipRepository.findByAttachedTo(invoice);
            assertThat(paperclips).isEmpty();

            final Invoice_attachSupportingDocument invoice_attachSupportingDocument = mixin(Invoice_attachSupportingDocument.class, invoice);

            // when
            final List<DocumentType> documentTypes = invoice_attachSupportingDocument.choices0$$();

            // then
            assertThat(documentTypes).hasSize(4);

            // when
            final List<String> roleNames = invoice_attachSupportingDocument.choices3$$();

            // then
            assertThat(roleNames).hasSize(1);

            // and when
            final DocumentType documentType = documentTypes.get(0);
            final String roleName = roleNames.get(0);
            final String fileName = "receipt-1.pdf";
            final Blob blob = asBlob(fileName);

            wrap(invoice_attachSupportingDocument).$$(documentType, blob, null, roleName);

            // then
            paperclips = paperclipRepository.findByAttachedTo(invoice);
            assertThat(paperclips).hasSize(1);

            final Paperclip paperclip = paperclips.get(0);
            final DocumentAbstract documentAbs = paperclip.getDocument();
            assertThat(documentAbs).isInstanceOf(Document.class);
            Document document = (Document) documentAbs;

            assertThat(documentAbs.getId()).isNotNull();
            assertThat(documentAbs.getSort()).isEqualTo(DocumentSort.BLOB);
            assertThat(documentAbs.getMimeType()).isEqualTo("application/pdf");
            assertThat(documentAbs.getName()).isEqualTo(fileName);
            assertThat(documentAbs.getAtPath()).isEqualTo(invoice.getApplicationTenancyPath());
            assertThat(documentAbs.getBlobBytes()).isEqualTo(blob.getBytes());
            assertThat(documentAbs.getType()).isEqualTo(documentType);
            assertThat(document.getRenderedAt()).isNotNull();
            assertThat(document.getCreatedAt()).isNotNull();

            final Object attachedTo = paperclip.getAttachedTo();
            assertThat(attachedTo).isSameAs(invoice);

            assertThat(paperclip.getRoleName()).isEqualTo(PaperclipRoleNames.SUPPORTING_DOCUMENT);
            assertThat(paperclip.getDocumentCreatedAt()).isEqualTo(document.getCreatedAt());
            assertThat(paperclip.getDocumentDate()).isEqualTo(document.getCreatedAt());
        }

        @Ignore // TODO
        @Test
        public void when_create_doc_then_attached_receipts_copied_over() throws Exception {

        }

    }

    public static class Invoice_delete_document_IntegTest extends Invoice_DocumentManagement_IntegTest {

        @Ignore // TODO
        @Test
        public void can_delete_documents_if_not_sent() throws Exception {

        }

        @Ignore // TODO
        @Test
        public void cannot_delete_documents_if_have_been_sent() throws Exception {

        }

    }

    public static class Invoice_sendByEmail_IntegTest extends Invoice_DocumentManagement_IntegTest {

        @Ignore // EST-1154
        @Test
        public void when_prelim_letter_any_invoice_receipts_attached_are_ignored() throws IOException {

            // given
            Invoice invoice = findInvoice(InvoiceStatus.NEW);
            DocAndCommForPrelimLetter prelimLetterViewModel = prelimLetterViewModelOf(invoice);
            assertNoDocumentOrCommunicationsFor(prelimLetterViewModel);

            // and given there is a receipt attached to the invoice
            receiptAttachedToInvoice(invoice, "receipt-1.pdf");

            invoice = findInvoice(InvoiceStatus.NEW);

            List<Paperclip> paperclips = paperclipRepository.findByAttachedTo(invoice);
            assertThat(paperclips).hasSize(1);
            final DocumentAbstract attachedReceipt = paperclips.get(0).getDocument();

            // when
            DocumentTemplate prelimLetterTemplate = findDocumentTemplateFor(DocumentTypeData.PRELIM_LETTER, invoice);
            wrap(mixin(Invoice_prepare.class, invoice)).$$(prelimLetterTemplate);
            Document document = prelimLetterOf(invoice);

            // (clearing queryResultsCache)
            invoice = findInvoice(InvoiceStatus.NEW);
            prelimLetterViewModel = prelimLetterViewModelOf(invoice);

            // then the newly created prelim letter doc
            Document prelimLetterDoc = mixin(DocAndCommForPrelimLetter_document.class, prelimLetterViewModel).$$();
            assertThat(prelimLetterDoc).isSameAs(document);
            assertThat(document.getState()).isEqualTo(DocumentState.RENDERED);

            assertThat(mixin(DocAndCommForPrelimLetter_communication.class, prelimLetterViewModel).$$()).isNull();

            // is attached to only invoice
            paperclips = paperclipRepository.findByDocument(prelimLetterDoc);
            assertThat(paperclips).hasSize(1);
            assertThat(paperclips).extracting(x -> x.getAttachedTo()).contains(invoice);

            // while the invoice itself now has two attachments (the original receipt and the newly created doc)
            paperclips = paperclipRepository.findByAttachedTo(invoice);
            assertThat(paperclips).hasSize(2);
            assertThat(paperclips).extracting(x -> x.getDocument()).contains(attachedReceipt, prelimLetterDoc);

            // and given there is another receipt attached to the invoice
            receiptAttachedToInvoice(invoice, "receipt-2.pdf");

            invoice = findInvoice(InvoiceStatus.NEW);

            // then the new receipt is attached to invoice but not to the prelimLetter
            paperclips = paperclipRepository.findByAttachedTo(invoice);
            assertThat(paperclips).hasSize(3);

            paperclips = paperclipRepository.findByDocument(prelimLetterDoc);
            assertThat(paperclips).hasSize(1);

            // and given
            final EmailAddress sendToEmailAddress = sendToFor(invoice, EmailAddress.class);

            // when
            final Communication communication =
                    wrap(mixin(Invoice_sendByEmail.class, invoice)).$$(document, sendToEmailAddress, null, null, null, null, null);

            invoice = findInvoice(InvoiceStatus.NEW);
            prelimLetterViewModel = prelimLetterViewModelOf(invoice);

            // then
            final Communication prelimLetterComm =
                    mixin(DocAndCommForPrelimLetter_communication.class, prelimLetterViewModel).$$();
            assertThat(prelimLetterComm).isSameAs(communication);

            assertThat(communication.getState()).isEqualTo(CommunicationState.PENDING);
            assertThat(prelimLetterComm.getCreatedAt()).isNotNull();
            assertThat(prelimLetterComm.getSentAt()).isNull();
            assertThat(prelimLetterComm.getSubject()).isEqualTo("OXF Poison Perfumeries Unit 3 Poison Preliminary Letter 01-01-2012.html");

            // and PL doc now also attached to comm, invoice.buyer and invoice.seller (as well as invoice)
            paperclips = paperclipRepository.findByDocument(prelimLetterDoc);
            assertThat(paperclips).hasSize(4);
            assertThat(paperclips).extracting(x -> x.getAttachedTo()).contains(invoice, invoice.getBuyer(), invoice.getSeller(), prelimLetterComm);

            // and comm attached to PL and also to a new covernote
            paperclips = paperclipRepository.findByAttachedTo(prelimLetterComm);
            assertThat(paperclips).hasSize(2);
            assertThat(paperclips).extracting(x -> x.getDocument()).contains(prelimLetterDoc);

        }

        @Ignore // EST-1154
        @Test
        public void when_invoice_doc_then_any_receipts_attached_are_included() throws IOException {

            // given an 'invoiced' invoice (so can create invoice notes for it)
            Invoice invoice = findInvoice(InvoiceStatus.NEW);
            approveAndInvoice(invoice);

            // without any document yet created
            DocAndCommForInvoiceDoc invoiceDocViewModel = invoiceDocViewModelOf(invoice);
            assertNoDocumentOrCommunicationsFor(invoiceDocViewModel);

            // and given there is a receipt attached to the invoice
            receiptAttachedToInvoice(invoice, "receipt-1.pdf");

            invoice = findInvoice(InvoiceStatus.INVOICED);
            List<Paperclip> paperclips = paperclipRepository.findByAttachedTo(invoice);
            assertThat(paperclips).hasSize(1);
            final DocumentAbstract attachedReceipt = paperclips.get(0).getDocument();

            // when
            DocumentTemplate prelimLetterTemplate = findDocumentTemplateFor(DocumentTypeData.INVOICE, invoice);
            wrap(mixin(Invoice_prepare.class, invoice)).$$(prelimLetterTemplate);
            Document document = invoiceDocOf(invoice);

            invoice = findInvoice(InvoiceStatus.INVOICED);
            invoiceDocViewModel = invoiceDocViewModelOf(invoice);

            // then the newly created invoice note doc
            Document invoiceDoc = mixin(DocAndCommForInvoiceDoc_document.class, invoiceDocViewModel).$$();
            assertThat(invoiceDoc).isSameAs(document);
            assertThat(document.getState()).isEqualTo(DocumentState.RENDERED);

            assertThat(mixin(DocAndCommForInvoiceDoc_communication.class, invoiceDocViewModel).$$()).isNull();

            // is attached to invoice and also the receipt
            paperclips = paperclipRepository.findByDocument(invoiceDoc);
            assertThat(paperclips).hasSize(2);
            assertThat(paperclips).extracting(x -> x.getAttachedTo()).contains(invoice, attachedReceipt);

            // while the invoice itself also has two attachments (the original receipt and the newly created doc)
            paperclips = paperclipRepository.findByAttachedTo(invoice);
            assertThat(paperclips).hasSize(2);
            assertThat(paperclips).extracting(x -> x.getDocument()).contains(attachedReceipt, invoiceDoc);

            // and given there is another receipt attached to the invoice
            receiptAttachedToInvoice(invoice, "receipt-2.pdf");

            invoice = findInvoice(InvoiceStatus.INVOICED);

            // then the new receipt is attached to invoice and also to the invoiceDoc
            paperclips = paperclipRepository.findByAttachedTo(invoice);
            assertThat(paperclips).hasSize(3);

            paperclips = paperclipRepository.findByDocument(invoiceDoc);
            assertThat(paperclips).hasSize(3);

            // and given
            final EmailAddress sendToEmailAddress = sendToFor(invoice, EmailAddress.class);

            // when
            final Communication communication =
                    wrap(mixin(Invoice_sendByEmail.class, invoice)).$$(document, sendToEmailAddress, null, null, null, null, null);

            invoice = findInvoice(InvoiceStatus.INVOICED);
            invoiceDocViewModel = invoiceDocViewModelOf(invoice);

            // then
            final Communication invoiceDocComm =
                    mixin(DocAndCommForInvoiceDoc_communication.class, invoiceDocViewModel).$$();
            assertThat(invoiceDocComm).isSameAs(communication);

            assertThat(communication.getState()).isEqualTo(CommunicationState.PENDING);
            assertThat(communication.getCreatedAt()).isNotNull();
            assertThat(communication.getSentAt()).isNull();
            assertThat(communication.getSubject()).isEqualTo("OXF Poison Perfumeries Unit 3 Poison Invoice 01-01-2012.html");

            // and InvNote doc now also attached to comm, invoice.buyer and invoice.seller (as well as invoice two receipts)
            paperclips = paperclipRepository.findByDocument(invoiceDoc);
            assertThat(paperclips).hasSize(6);
            assertThat(paperclips).extracting(x -> x.getAttachedTo()).contains(invoice, invoice.getBuyer(), invoice.getSeller());

            // and comm attached to PL, also to a new covernote, AND ALSO to both of the receipts
            paperclips = paperclipRepository.findByAttachedTo(invoiceDocComm);
            assertThat(paperclips).hasSize(4);
            assertThat(paperclips)
                    .extracting(Paperclip::getDocument)
                    .filteredOn(x -> DocumentTypeData.SUPPLIER_RECEIPT.isDocTypeFor(document))
                    .hasSize(2);

        }

    }


    public static class Invoice_sendByPost_IntegTest extends Invoice_DocumentManagement_IntegTest {

        @Ignore // EST-1154
        @Test
        public void when_prelim_letter_any_invoice_receipts_attached_are_ignored() throws IOException {

            // given
            Invoice invoice = findInvoice(InvoiceStatus.NEW);
            DocAndCommForPrelimLetter prelimLetterViewModel = prelimLetterViewModelOf(invoice);
            assertNoDocumentOrCommunicationsFor(prelimLetterViewModel);

            // and given there is a receipt attached to the invoice
            receiptAttachedToInvoice(invoice, "receipt-1.pdf");

            invoice = findInvoice(InvoiceStatus.NEW);

            List<Paperclip> paperclips = paperclipRepository.findByAttachedTo(invoice);
            assertThat(paperclips).hasSize(1);
            final DocumentAbstract attachedReceipt = paperclips.get(0).getDocument();

            // when
            DocumentTemplate prelimLetterTemplate = findDocumentTemplateFor(DocumentTypeData.PRELIM_LETTER, invoice);
            wrap(mixin(Invoice_prepare.class, invoice)).$$(prelimLetterTemplate);
            Document document = prelimLetterOf(invoice);

            // (clearing queryResultsCache)
            invoice = findInvoice(InvoiceStatus.NEW);
            prelimLetterViewModel = prelimLetterViewModelOf(invoice);

            // then the newly created prelim letter doc
            Document prelimLetterDoc = mixin(DocAndCommForPrelimLetter_document.class, prelimLetterViewModel).$$();
            assertThat(prelimLetterDoc).isSameAs(document);
            assertThat(document.getState()).isEqualTo(DocumentState.RENDERED);

            assertThat(mixin(DocAndCommForPrelimLetter_communication.class, prelimLetterViewModel).$$()).isNull();

            // is attached to only invoice
            paperclips = paperclipRepository.findByDocument(prelimLetterDoc);
            assertThat(paperclips).hasSize(1);
            assertThat(paperclips).extracting(x -> x.getAttachedTo()).contains(invoice);

            // while the invoice itself now has two attachments (the original receipt and the newly created doc)
            paperclips = paperclipRepository.findByAttachedTo(invoice);
            assertThat(paperclips).hasSize(2);
            assertThat(paperclips).extracting(x -> x.getDocument()).contains(attachedReceipt, prelimLetterDoc);

            // and given
            PostalAddress sendTo = sendToFor(invoice, PostalAddress.class);

            // when
            Blob downloaded = wrap(mixin(Invoice_sendByPost.class, invoice)).$$(document, sendTo);

            invoice = findInvoice(InvoiceStatus.NEW);
            prelimLetterViewModel = prelimLetterViewModelOf(invoice);

            // then the bytes of downloaded are at least as many as that of the original document (receipt is ignored)
            assertThat(downloaded).isNotNull();
            assertThat(downloaded.getBytes().length).isCloseTo(document.getBlobBytes().length, Percentage.withPercentage(10));

            final Communication prelimLetterComm =
                    mixin(DocAndCommForPrelimLetter_communication.class, prelimLetterViewModel).$$();

            // then the comm is automatically sent
            assertThat(prelimLetterComm.getState()).isEqualTo(CommunicationState.SENT);
            assertThat(prelimLetterComm.getCreatedAt()).isNull();
            assertThat(prelimLetterComm.getSentAt()).isNotNull();
            assertThat(prelimLetterComm.getSubject()).startsWith("Preliminary letter for Invoice Temp *000");

            // and PL doc now also attached to comm, invoice.buyer and invoice.seller (as well as invoice)
            paperclips = paperclipRepository.findByDocument(prelimLetterDoc);
            assertThat(paperclips).hasSize(4);
            assertThat(paperclips).extracting(x -> x.getAttachedTo()).contains(invoice, invoice.getBuyer(), invoice.getSeller(), prelimLetterComm);

            // and comm attached to PL
            paperclips = paperclipRepository.findByAttachedTo(prelimLetterComm);
            assertThat(paperclips).hasSize(1);
            assertThat(paperclips).extracting(x -> x.getDocument()).contains(prelimLetterDoc);
        }

        @Ignore // EST-1154
        @Test
        public void when_invoice_doc_then_any_receipts_attached_are_included() throws IOException {

            // given an 'invoiced' invoice (so can create invoice notes for it)
            Invoice invoice = findInvoice(InvoiceStatus.NEW);
            approveAndInvoice(invoice);

            // without any document yet created
            DocAndCommForInvoiceDoc invoiceDocViewModel = invoiceDocViewModelOf(invoice);
            assertNoDocumentOrCommunicationsFor(invoiceDocViewModel);

            // and given there is a receipt attached to the invoice
            receiptAttachedToInvoice(invoice, "receipt-1.pdf");

            invoice = findInvoice(InvoiceStatus.INVOICED);
            List<Paperclip> paperclips = paperclipRepository.findByAttachedTo(invoice);
            assertThat(paperclips).hasSize(1);
            final DocumentAbstract attachedReceipt = paperclips.get(0).getDocument();

            // when
            DocumentTemplate prelimLetterTemplate = findDocumentTemplateFor(DocumentTypeData.INVOICE, invoice);
            wrap(mixin(Invoice_prepare.class, invoice)).$$(prelimLetterTemplate);
            Document document = invoiceDocOf(invoice);

            invoice = findInvoice(InvoiceStatus.INVOICED);
            invoiceDocViewModel = invoiceDocViewModelOf(invoice);

            // then the newly created invoice note doc
            Document invoiceDoc = mixin(DocAndCommForInvoiceDoc_document.class, invoiceDocViewModel).$$();
            assertThat(invoiceDoc).isSameAs(document);
            assertThat(document.getState()).isEqualTo(DocumentState.RENDERED);

            assertThat(mixin(DocAndCommForInvoiceDoc_communication.class, invoiceDocViewModel).$$()).isNull();

            // is attached to invoice and also the receipt
            paperclips = paperclipRepository.findByDocument(invoiceDoc);
            assertThat(paperclips).hasSize(2);
            assertThat(paperclips).extracting(x -> x.getAttachedTo()).contains(invoice, attachedReceipt);

            // while the invoice itself also has two attachments (the original receipt and the newly created doc)
            paperclips = paperclipRepository.findByAttachedTo(invoice);
            assertThat(paperclips).hasSize(2);
            assertThat(paperclips).extracting(x -> x.getDocument()).contains(attachedReceipt, invoiceDoc);


            // and when

            // and given
            PostalAddress sendTo = sendToFor(invoice, PostalAddress.class);

            // when
            Blob downloaded = wrap(mixin(Invoice_sendByPost.class, invoice)).$$(document, sendTo);

            // then we get more bytes than the original document (includes the receipt)
            assertThat(downloaded).isNotNull();
            assertThat(downloaded.getBytes().length).isGreaterThan(document.getBlobBytes().length);

            invoice = findInvoice(InvoiceStatus.INVOICED);
            invoiceDocViewModel = invoiceDocViewModelOf(invoice);

            // then
            final Communication invoiceDocComm =
                    mixin(DocAndCommForInvoiceDoc_communication.class, invoiceDocViewModel).$$();

            // then the comm is automatically sent
            assertThat(invoiceDocComm.getState()).isEqualTo(CommunicationState.SENT);
            assertThat(invoiceDocComm.getCreatedAt()).isNull();
            assertThat(invoiceDocComm.getSentAt()).isNotNull();
            assertThat(invoiceDocComm.getSubject()).isEqualTo("Invoice for OXF-000001.pdf");

            // and InvNote doc now also attached to comm, invoice.buyer and invoice.seller (as well as invoice and receipt)
            paperclips = paperclipRepository.findByDocument(invoiceDoc);
            assertThat(paperclips).hasSize(5);
            assertThat(paperclips).extracting(x -> x.getAttachedTo()).contains(invoice, invoice.getBuyer(), invoice.getSeller(), invoiceDocComm, attachedReceipt);

            // and comm attached to PL, also to a new covernote, AND ALSO to the original attached receipt
            paperclips = paperclipRepository.findByAttachedTo(invoiceDocComm);
            assertThat(paperclips).hasSize(2);
            assertThat(paperclips).extracting(x -> x.getDocument()).contains(invoiceDoc, attachedReceipt);

        }

    }


    public static class Invoice_remove_IntegTest extends Invoice_DocumentManagement_IntegTest {

        @Test
        public void when_has_associated_document_that_has_not_been_sent() throws Exception {

            // given
            Invoice invoice = findInvoice(InvoiceStatus.NEW);
            assertThat(invoice).isNotNull();
            assertThat(invoice.getStatus().invoiceIsChangable()).isTrue();

            // and given have a PL doc
            DocumentTemplate prelimLetterTemplate = findDocumentTemplateFor(DocumentTypeData.PRELIM_LETTER, invoice);
            wrap(mixin(Invoice_prepare.class, invoice)).$$(prelimLetterTemplate);
            Document document = prelimLetterOf(invoice);
            assertThat(document).isNotNull();

            // and given is attached to only invoice
            List<Paperclip> paperclips = paperclipRepository.findByDocument(document);
            assertThat(paperclips).hasSize(1);

            assertThat(paperclips).extracting(x -> x.getAttachedTo()).contains(invoice);

            final Bookmark documentBookmark = bookmarkService.bookmarkFor(document);

            // when remove the invoice
            wrap(mixin(Invoice._remove.class, invoice)).exec();

            transactionService.nextTransaction();

            // then (deletes invoice, its paperclip)
            invoice = findInvoice(InvoiceStatus.NEW);
            assertThat(invoice).isNull();

            // and expect (to have deleted associated document too)
            expectedExceptions.expectMessage("only resolve object that is persistent");

            // when
            bookmarkService.lookup(documentBookmark);
        }

        @Test
        public void when_has_associated_document_that_HAS_been_sent() throws Exception {

            // given
            Invoice invoice = findInvoice(InvoiceStatus.NEW);
            assertThat(invoice).isNotNull();
            assertThat(invoice.getStatus().invoiceIsChangable()).isTrue();

            // and given have a PL doc
            DocumentTemplate prelimLetterTemplate = findDocumentTemplateFor(DocumentTypeData.PRELIM_LETTER, invoice);
            wrap(mixin(Invoice_prepare.class, invoice)).$$(prelimLetterTemplate);
            Document document = prelimLetterOf(invoice);
            assertThat(document).isNotNull();

            // and given document sent
            final CommunicationChannel sendTo = invoice.getSendTo();
            assertThat(sendTo).isInstanceOf(EmailAddress.class);
            mixin(Invoice_sendByEmail.class, invoice).$$(document, (EmailAddress)sendTo, null, null, null, null, null);

            transactionService.flushTransaction();

            // and given is attached to invoice, buyer and seller and the comm
            List<Paperclip> paperclips = paperclipRepository.findByDocument(document);
            assertThat(paperclips).hasSize(4);

            final Party invoiceSeller = invoice.getSeller();
            final Party invoiceBuyer = invoice.getBuyer();
            final Optional<Communication> commIfAny =
                    paperclips.stream()
                            .map(x -> x.getAttachedTo())
                            .filter(x -> x instanceof Communication)
                            .map(Communication.class::cast)
                            .findFirst();
            assertThat(commIfAny.isPresent()).isTrue();
            final Communication communication = commIfAny.get();

            assertThat(paperclips)
                    .extracting(x -> x.getAttachedTo())
                    .contains(invoice, invoiceBuyer, invoiceSeller, communication);

            // when remove the invoice
            wrap(mixin(Invoice._remove.class, invoice)).exec();

            transactionService.flushTransaction();

            // then (deletes invoice, one of its paperclips)
            invoice = findInvoice(InvoiceStatus.NEW);
            assertThat(invoice).isNull();

            // but still attached to buyer, seller and communication
            assertThat(paperclipRepository.findByAttachedTo(invoiceBuyer)).extracting(x -> x.getDocument()).contains(document);
            assertThat(paperclipRepository.findByAttachedTo(invoiceSeller)).extracting(x -> x.getDocument()).contains(document);
            assertThat(paperclipRepository.findByAttachedTo(communication)).extracting(x -> x.getDocument()).contains(document);

            // and document still exists
            final Bookmark documentBookmark = bookmarkService.bookmarkFor(document);
            transactionService.nextTransaction();

            // (ie no exception thrown)
            bookmarkService.lookup(documentBookmark);
        }

    }


    public static class Document_delete_IntegTest extends Invoice_DocumentManagement_IntegTest {

        @Test
        public void can_delete_document_when_not_been_sent() throws Exception {

            // given
            Invoice invoice = findInvoice(InvoiceStatus.NEW);
            assertThat(invoice).isNotNull();
            assertThat(invoice.getStatus().invoiceIsChangable()).isTrue();

            // and given have a PL doc
            DocumentTemplate prelimLetterTemplate = findDocumentTemplateFor(DocumentTypeData.PRELIM_LETTER, invoice);
            wrap(mixin(Invoice_prepare.class, invoice)).$$(prelimLetterTemplate);
            Document document = prelimLetterOf(invoice);

            assertThat(document).isNotNull();

            assertThat(paperclipRepository.findByAttachedTo(invoice)).hasSize(1);

            // when
            final Bookmark documentBookmark = bookmarkService.bookmarkFor(document);

            wrap(mixin(Document_delete.class, document)).$$();

            transactionService.nextTransaction();

            // then expect
            expectedExceptions.expectMessage("only resolve object that is persistent");

            // when attempt to find
            bookmarkService.lookup(documentBookmark);

        }

        @Test
        public void canNOT_delete_document_once_it_has_been_sent() throws Exception {

            // given
            Invoice invoice = findInvoice(InvoiceStatus.NEW);
            assertThat(invoice).isNotNull();
            assertThat(invoice.getStatus().invoiceIsChangable()).isTrue();

            // and given have a PL doc
            DocumentTemplate prelimLetterTemplate = findDocumentTemplateFor(DocumentTypeData.PRELIM_LETTER, invoice);
            wrap(mixin(Invoice_prepare.class, invoice)).$$(prelimLetterTemplate);
            Document document = prelimLetterOf(invoice);

            // and given document sent
            final CommunicationChannel sendTo = invoice.getSendTo();
            assertThat(sendTo).isInstanceOf(EmailAddress.class);
            mixin(Invoice_sendByEmail.class, invoice).$$(document, (EmailAddress)sendTo, null, null, null, null, null);

            transactionService.flushTransaction();

            // then expect
            expectedExceptions.expectMessage("Document has already been sent as a communication");

            // when attempt to
            wrap(mixin(Document_delete.class, document)).$$();
        }

    }


    public static class Paperclip_changeRole_IntegTest extends Invoice_DocumentManagement_IntegTest {

        @Test
        public void for_prelim_letter() throws Exception {

            // given
            Invoice invoice = findInvoice(InvoiceStatus.NEW);
            DocumentTemplate prelimLetterTemplate = findDocumentTemplateFor(DocumentTypeData.PRELIM_LETTER, invoice);

            // and given
            wrap(mixin(Invoice_prepare.class, invoice)).$$(prelimLetterTemplate);
            Document document = prelimLetterOf(invoice);
            assertThat(document).isNotNull();

            // and given
            List<Paperclip> paperclips = paperclipRepository.findByDocument(document);
            assertThat(paperclips).hasSize(1);
            final Paperclip paperclip = paperclips.get(0);

            // expect
            expectedExceptions.expect(HiddenException.class);

            // when
            wrap(mixin(Paperclip_changeRole.class, paperclip)).$$("new role");
        }
    }


    //region > helpers

    private List<InvoiceForLease> findMatchingInvoices(
            final Party seller,
            final Party buyer,
            final Lease lease,
            final LocalDate invoiceStartDate, final InvoiceStatus invoiceStatus) {
        return invoiceForLeaseRepository.findMatchingInvoices(
                seller, buyer, PaymentMethod.DIRECT_DEBIT,
                lease, invoiceStatus,
                invoiceStartDate);
    }

    Invoice findInvoice(final InvoiceStatus invoiceStatus) {

        // clears out queryResultsCache
        transactionService.nextTransaction();

        final Party seller = partyRepository
                .findPartyByReference(InvoiceForLeaseItemTypeOfRentOneQuarterForOxfPoison003.PARTY_REF_SELLER);
        final Party buyer = partyRepository
                .findPartyByReference(InvoiceForLeaseItemTypeOfRentOneQuarterForOxfPoison003.PARTY_REF_BUYER);
        final Lease lease = leaseRepository
                .findLeaseByReference(InvoiceForLeaseItemTypeOfRentOneQuarterForOxfPoison003.LEASE_REF);
        final LocalDate invoiceStartDate = InvoiceForLeaseItemTypeOfRentOneQuarterForOxfPoison003
                .startDateFor(lease);

        List<InvoiceForLease> matchingInvoices = findMatchingInvoices(seller, buyer, lease, invoiceStartDate, invoiceStatus);
        assertThat(matchingInvoices.size()).isLessThanOrEqualTo(1);
        return matchingInvoices.isEmpty() ? null : matchingInvoices.get(0);
    }

    DocumentTemplate findDocumentTemplateFor(final DocumentTypeData documentTypeData, final Invoice invoice) {
        final DocumentType documentType = documentTypeData.findUsing(documentTypeRepository);
        assertThat(documentType).isNotNull();
        DocumentTemplate documentTemplate = documentTemplateRepository.findFirstByTypeAndApplicableToAtPath(documentType, invoice.getApplicationTenancyPath());
        assertThat(documentType).isNotNull();
        return documentTemplate;
    }

    DocAndCommForPrelimLetter prelimLetterViewModelOf(final Invoice invoice) {
        List<DocAndCommForPrelimLetter> prelimLetterViewModels = mixin(Invoice_ForLease_preliminaryLetters.class, invoice).$$();
        assertThat(prelimLetterViewModels).hasSize(1);
        return prelimLetterViewModels.get(0);
    }

    DocAndCommForInvoiceDoc invoiceDocViewModelOf(final Invoice invoice) {
        List<DocAndCommForInvoiceDoc> invoiceDocViewModels = mixin(Invoice_invoiceDocs.class, invoice).$$();
        assertThat(invoiceDocViewModels).hasSize(1);
        return invoiceDocViewModels.get(0);
    }

    static Blob asBlob(final String fileName) throws IOException {
        final URL url = Resources.getResource(Invoice_DocumentManagement_IntegTest.class, fileName);
        final byte[] bytes = Resources.toByteArray(url);
        return new Blob(fileName, "application/pdf", bytes);
    }

    void approveAndInvoice(final Invoice invoice) {
        wrap(mixin(InvoiceForLease._approve.class, invoice)).$$();
        wrap(mixin(InvoiceForLease._invoice.class, invoice)).$$(invoice.getDueDate().minusDays(1));
    }

    Document prelimLetterOf(final Invoice invoice) {
        final List<DocAndCommForPrelimLetter> viewModels = mixin(Invoice_ForLease_preliminaryLetters.class, invoice).$$();
        assertThat(viewModels).hasSize(1);
        final DocAndCommForPrelimLetter viewModel = viewModels.get(0);
        final Document document = mixin(DocAndCommForPrelimLetter_document.class, viewModel).$$();
        assertThat(document).isNotNull();
        return document;
    }

    Document invoiceDocOf(final Invoice invoice) {
        final List<DocAndCommForInvoiceDoc> viewModels = mixin(Invoice_invoiceDocs.class, invoice).$$();
        assertThat(viewModels).hasSize(1);
        final DocAndCommForInvoiceDoc viewModel = viewModels.get(0);
        final Document document = mixin(DocAndCommForInvoiceDoc_document.class, viewModel).$$();
        assertThat(document).isNotNull();
        return document;
    }

    void receiptAttachedToInvoice(final Invoice invoice, final String fileName) throws IOException {

        final Invoice_attachSupportingDocument invoice_attachSupportingDocument = mixin(Invoice_attachSupportingDocument.class, invoice);

        final List<DocumentType> documentTypes = invoice_attachSupportingDocument.choices0$$();
        assertThat(documentTypes).hasSize(2);
        final DocumentType documentType = documentTypes.get(0);
        final List<String> roleNames = invoice_attachSupportingDocument.choices3$$();
        assertThat(roleNames).hasSize(1);
        final String roleName = roleNames.get(0);

        final Blob blob = asBlob(fileName);

        wrap(invoice_attachSupportingDocument).$$(documentType, blob, null, roleName);
    }

    void assertNoDocumentOrCommunicationsFor(final DocAndCommForPrelimLetter prelimLetterViewModel) {
        assertNoDocumentFor(prelimLetterViewModel);
        assertNoCommunicationFor(prelimLetterViewModel);
    }

    void assertNoCommunicationFor(final DocAndCommForPrelimLetter prelimLetterViewModel) {
        assertThat(mixin(DocAndCommForPrelimLetter_communication.class, prelimLetterViewModel).$$()).isNull();
        assertThat(mixin(DocAndCommForPrelimLetter_communicationState.class, prelimLetterViewModel).$$()).isNull();
    }

    void assertNoDocumentFor(final DocAndCommForPrelimLetter prelimLetterViewModel) {
        assertThat(mixin(DocAndCommForPrelimLetter_document.class, prelimLetterViewModel).$$()).isNull();
        assertThat(mixin(DocAndCommForPrelimLetter_documentState.class, prelimLetterViewModel).$$()).isNull();
    }

    void assertNoDocumentOrCommunicationsFor(final DocAndCommForInvoiceDoc invoiceDocViewModel) {
        assertNoDocumentFor(invoiceDocViewModel);
        assertNoCommunicationFor(invoiceDocViewModel);
    }

    void assertNoCommunicationFor(final DocAndCommForInvoiceDoc invoiceDocViewModel) {
        assertThat(mixin(DocAndCommForInvoiceDoc_communication.class, invoiceDocViewModel).$$()).isNull();
        assertThat(mixin(DocAndCommForInvoiceDoc_communicationState.class, invoiceDocViewModel).$$()).isNull();
    }

    void assertNoDocumentFor(final DocAndCommForInvoiceDoc invoiceDocViewModel) {
        assertThat(mixin(DocAndCommForInvoiceDoc_document.class, invoiceDocViewModel).$$()).isNull();
        assertThat(mixin(DocAndCommForInvoiceDoc_documentState.class, invoiceDocViewModel).$$()).isNull();
    }

    <T extends CommunicationChannel> T sendToFor(final Invoice invoice, final Class<T> communicationChannelClass)  {
        final CommunicationChannel sendTo = invoice.getSendTo();
        if(communicationChannelClass.isAssignableFrom(sendTo.getClass())) {
            return communicationChannelClass.cast(sendTo);
        }

        final List<CommunicationChannel> communicationChannels =
                mixin(InvoiceForLease_overrideSendTo.class, invoice).choices0$$();
        final Optional<CommunicationChannel> commChannelIfAny =
                communicationChannels.stream()
                        .filter(x -> communicationChannelClass.isAssignableFrom(x.getClass()))
                        .findFirst();
        if (!commChannelIfAny.isPresent()) {
            throw new IllegalStateException(
                    "could not locate communication channel of type " + communicationChannelClass.getSimpleName());
        }

        final CommunicationChannel communicationChannel = commChannelIfAny.get();
        wrap(mixin(InvoiceForLease_overrideSendTo.class, invoice)).$$(communicationChannel);
        return communicationChannelClass.cast(communicationChannel);
    }


    //endregion

    //region > injected services

    @Inject
    BookmarkService2 bookmarkService;

    @Inject
    InvoiceRepository invoiceRepository;
    @Inject
    InvoiceForLeaseRepository invoiceForLeaseRepository;
    @Inject
    PartyRepository partyRepository;
    @Inject
    LeaseRepository leaseRepository;

    @Inject
    DocumentTemplateRepository documentTemplateRepository;
    @Inject
    DocumentTypeRepository documentTypeRepository;

    @Inject
    PaperclipRepository paperclipRepository;
    @Inject
    TransactionService transactionService;

    @Inject
    ClockService clockService;
    //endregion

}
