package org.estatio.integtests.budget;

import org.apache.isis.applib.fixturescripts.FixtureScript;
import org.estatio.dom.asset.Property;
import org.estatio.dom.asset.PropertyRepository;
import org.estatio.dom.budgeting.budget.Budget;
import org.estatio.dom.budgeting.budget.BudgetRepository;
import org.estatio.dom.budgeting.keytable.KeyTable;
import org.estatio.dom.budgeting.keytable.KeyTableRepository;
import org.estatio.fixture.EstatioBaseLineFixture;
import org.estatio.fixture.asset.PropertyForOxfGb;
import org.estatio.fixture.budget.BudgetsForOxf;
import org.estatio.fixture.budget.KeyTablesForOxf;
import org.estatio.integtests.EstatioIntegrationTest;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;

import javax.inject.Inject;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by jodo on 19/08/15.
 */
public class KeyTableRepositoryTest extends EstatioIntegrationTest {

    @Inject
    KeyTableRepository keyTableRepository;

    @Inject
    PropertyRepository propertyRepository;

    @Inject

    @Before
    public void setupData() {
        runFixtureScript(new FixtureScript() {
            @Override
            protected void execute(final ExecutionContext executionContext) {
                executionContext.executeChild(this, new EstatioBaseLineFixture());
                executionContext.executeChild(this, new BudgetsForOxf());
                executionContext.executeChild(this, new KeyTablesForOxf());
            }
        });
    }

    public static class FindByProperty extends KeyTableRepositoryTest {

        @Test
        public void happyCase() throws Exception {
            // given
            Property property = propertyRepository.findPropertyByReference(PropertyForOxfGb.REF);
            // when
            final List<KeyTable> keyTableList = keyTableRepository.findByProperty(property);
            // then
            assertThat(keyTableList.size()).isEqualTo(2);

        }

    }

    public static class FindByPropertyAndNameAndStartDate extends KeyTableRepositoryTest {

        @Test
        public void happyCase() throws Exception {
            // given
            Property property = propertyRepository.findPropertyByReference(PropertyForOxfGb.REF);
            // when
            final KeyTable keyTable = keyTableRepository.findByPropertyAndNameAndStartDate(property, KeyTablesForOxf.NAME, KeyTablesForOxf.STARTDATE);
            // then
            assertThat(keyTable.getName()).isEqualTo(KeyTablesForOxf.NAME);
            assertThat(keyTable.getProperty()).isEqualTo(property);

        }

    }

    public static class FindByPropertyAndStartDateAndEndDate extends KeyTableRepositoryTest {

        @Test
        public void happyCase() throws Exception {
            // given
            Property property = propertyRepository.findPropertyByReference(PropertyForOxfGb.REF);
            // when
            final List<KeyTable> keyTables = keyTableRepository.findByPropertyAndStartDateAndEndDate(property, KeyTablesForOxf.STARTDATE, KeyTablesForOxf.ENDDATE);
            // then
            assertThat(keyTables.size()).isEqualTo(2);
            assertThat(keyTables.get(0).getProperty()).isEqualTo(property);

        }

    }

    public static class FindByBudget extends KeyTableRepositoryTest {

        @Test
        public void happyCase() throws Exception {
            // given
            Property property = propertyRepository.findPropertyByReference(PropertyForOxfGb.REF);
            Budget budget = budgetRepository.findByPropertyAndStartDate(property, new LocalDate(2015,01,01));

            // when
            final List<KeyTable> keyTables = keyTableRepository.findByBudget(budget);
            // then
            assertThat(keyTables.size()).isEqualTo(2);
            assertThat(keyTables.get(0).getProperty()).isEqualTo(property);
            assertThat(keyTables.get(0).getStartDate()).isEqualTo(budget.getStartDate());
            assertThat(keyTables.get(0).getEndDate()).isEqualTo(budget.getEndDate());
        }

        @Inject
        private BudgetRepository budgetRepository;
    }

}