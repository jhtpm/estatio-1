package org.estatio.capex.dom.documents.categorisation.tasks;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.apache.isis.applib.annotation.Action;
import org.apache.isis.applib.annotation.ActionLayout;
import org.apache.isis.applib.annotation.Contributed;
import org.apache.isis.applib.annotation.Mixin;

import org.incode.module.document.dom.impl.docs.Document;

import org.estatio.capex.dom.documents.categorisation.document.Document_categoriseAsOrder;
import org.estatio.capex.dom.documents.categorisation.IncomingDocumentCategorisationStateTransition;
import org.estatio.capex.dom.task.Task;
import org.estatio.capex.dom.task.Task_mixinActAbstract;
import org.estatio.dom.asset.Property;

@Mixin(method = "act")
public class Task_categoriseAsOrder
        extends
        Task_mixinActAbstract<Document_categoriseAsOrder, Document> {

    protected final Task task;

    public Task_categoriseAsOrder(final Task task) {
        super(task, Document_categoriseAsOrder.class);
        this.task = task;
    }

    @Action()
    @ActionLayout(contributed = Contributed.AS_ACTION)
    public Object act(
            @Nullable final Property property,
            @Nullable final String comment,
            final boolean goToNext) {
        Object mixinResult = mixin().act(property, comment);
        return toReturnElse(goToNext, mixinResult);
    }

    public boolean default2Act() {
        return true;
    }

    public boolean hideAct() {
        return super.hideAct() || mixin().hideAct();
    }

    @Override
    protected Document doGetDomainObjectIfAny() {
        final IncomingDocumentCategorisationStateTransition transition = repository.findByTask(this.task);
        return transition != null ? transition.getDocument() : null;
    }

    @Inject
    IncomingDocumentCategorisationStateTransition.Repository repository;

}
