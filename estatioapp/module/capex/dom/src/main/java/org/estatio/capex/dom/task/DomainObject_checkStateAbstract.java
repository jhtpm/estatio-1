package org.estatio.capex.dom.task;

import javax.inject.Inject;

import org.apache.isis.applib.annotation.Action;
import org.apache.isis.applib.annotation.ActionLayout;
import org.apache.isis.applib.annotation.Contributed;
import org.apache.isis.applib.annotation.SemanticsOf;

import org.estatio.capex.dom.state.State;
import org.estatio.capex.dom.state.StateTransitionAbstract;
import org.estatio.capex.dom.state.StateTransitionService;
import org.estatio.capex.dom.state.StateTransitionType;

/**
 * Subclasses should be annotated using: @Mixin(method = "act")
 */
public abstract class DomainObject_checkStateAbstract<
        DO,
        ST extends StateTransitionAbstract<DO, ST, STT, S>,
        STT extends StateTransitionType<DO, ST, STT, S>,
        S extends State<S>
        > {

    protected final DO domainObject;
    private final Class<ST> stateTransitionClass;

    public DomainObject_checkStateAbstract(final DO domainObject, final Class<ST> stateTransitionClass) {
        this.domainObject = domainObject;
        this.stateTransitionClass = stateTransitionClass;
    }

    @Action(semantics = SemanticsOf.IDEMPOTENT)
    @ActionLayout(contributed= Contributed.AS_ACTION)
    public DO act() {

        //stateTransitionService.checkState(domainObject, stateTransitionClass);
        stateTransitionService.trigger(domainObject, stateTransitionClass, null, null);

        return domainObject;
    }

    public boolean hideAct() {
        ST st = stateTransitionService.pendingTransitionOf(domainObject, stateTransitionClass);
        return st == null;
    }

    @Inject
    StateTransitionService stateTransitionService;

}
