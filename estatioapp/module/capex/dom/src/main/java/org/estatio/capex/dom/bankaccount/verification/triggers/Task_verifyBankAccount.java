package org.estatio.capex.dom.bankaccount.verification.triggers;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.apache.isis.applib.annotation.Action;
import org.apache.isis.applib.annotation.ActionLayout;
import org.apache.isis.applib.annotation.Contributed;
import org.apache.isis.applib.annotation.Mixin;
import org.apache.isis.applib.annotation.SemanticsOf;

import org.estatio.capex.dom.EstatioCapexDomModule;
import org.estatio.capex.dom.bankaccount.verification.BankAccountVerificationStateTransition;
import org.estatio.capex.dom.task.Task;
import org.estatio.dom.financial.bankaccount.BankAccount;

@Mixin(method = "act")
public class Task_verifyBankAccount
        extends
        Task._mixinAbstract<BankAccount_verify, BankAccount> {

    protected final Task task;

    public Task_verifyBankAccount(final Task task) {
        super(task, BankAccount_verify.class);
        this.task = task;
    }

    public static class DomainEvent
            extends EstatioCapexDomModule.ActionDomainEvent<Task_verifyBankAccount> {
    }

    @Action(
            semantics = SemanticsOf.IDEMPOTENT,
            domainEvent = DomainEvent.class)
    @ActionLayout(contributed = Contributed.AS_ACTION)
    public Task act(@Nullable final String comment, final boolean goToNext) {
        mixin().act(comment);
        return taskToReturn(goToNext, task);
    }

    public boolean hideAct() {
        return super.hideAct() || mixin().hideAct();
    }

    @Override
    protected BankAccount doGetDomainObjectIfAny() {
        final BankAccountVerificationStateTransition transition = repository.findByTask(this.task);
        return transition != null ? transition.getBankAccount() : null;
    }

    @Inject
    BankAccountVerificationStateTransition.Repository repository;

}
