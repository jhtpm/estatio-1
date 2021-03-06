package org.estatio.capex.fixture.charge;

import javax.inject.Inject;

import org.apache.isis.applib.fixturescripts.FixtureScript;

import org.isisaddons.module.excel.dom.ExcelFixture2;
import org.isisaddons.module.excel.dom.FixtureAwareRowHandler;

import org.estatio.dom.charge.Applicability;
import org.estatio.dom.charge.Charge;
import org.estatio.dom.charge.ChargeRepository;

import lombok.Getter;
import lombok.Setter;

public class IncomingChargeHandler implements FixtureAwareRowHandler<IncomingChargeHandler> {

    /**
     * Also used as the reference and as the description.
     */
    @Getter @Setter
    private String name;

    /**
     * The name/reference of the parent.
     */
    @Getter @Setter
    private String parent;

    @Getter @Setter
    private String atPath;

    /**
     * To allow for usage within fixture scripts also.
     */
    @Setter
    private FixtureScript.ExecutionContext executionContext;

    /**
     * To allow for usage within fixture scripts also.
     */
    @Setter
    private ExcelFixture2 excelFixture2;


    @Override
    public void handleRow(final IncomingChargeHandler previousRow) {

        if(executionContext != null && excelFixture2 != null) {
            executionContext.addResult(excelFixture2, this.handle(previousRow));
        }

    }

    private Charge handle(final IncomingChargeHandler previousRow) {
        final Charge parentObj = chargeRepository.findByReference(parent);

        return chargeRepository.upsert(name, parentObj, atPath, Applicability.INCOMING);
    }

    @Inject
    ChargeRepository chargeRepository;

}

