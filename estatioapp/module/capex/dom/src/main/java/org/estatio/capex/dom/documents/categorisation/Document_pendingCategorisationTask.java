package org.estatio.capex.dom.documents.categorisation;

import org.apache.isis.applib.annotation.Mixin;

import org.incode.module.document.dom.impl.docs.Document;

import org.estatio.capex.dom.documents.categorisation.IncomingDocumentCategorisationState;
import org.estatio.capex.dom.documents.categorisation.IncomingDocumentCategorisationStateTransition;
import org.estatio.capex.dom.documents.categorisation.IncomingDocumentCategorisationStateTransitionType;
import org.estatio.capex.dom.task.DomainObject_pendingTaskAbstract;
import org.estatio.dom.invoice.DocumentTypeData;

@Mixin(method="prop")
public class Document_pendingCategorisationTask
        extends DomainObject_pendingTaskAbstract<
                    Document,
                    IncomingDocumentCategorisationStateTransition,
                    IncomingDocumentCategorisationStateTransitionType,
                    IncomingDocumentCategorisationState> {


    public Document_pendingCategorisationTask(final Document document) {
        super(document, IncomingDocumentCategorisationStateTransition.class);
    }

    public boolean hideProp() {
        return !DocumentTypeData.hasIncomingType(domainObject);
    }

}
