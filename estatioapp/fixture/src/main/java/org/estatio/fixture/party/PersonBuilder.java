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
package org.estatio.fixture.party;

import java.util.List;

import javax.inject.Inject;

import org.assertj.core.util.Lists;

import org.apache.isis.applib.value.Password;

import org.isisaddons.module.security.dom.user.ApplicationUser;
import org.isisaddons.module.security.dom.user.ApplicationUserRepository;

import org.estatio.dom.asset.Property;
import org.estatio.dom.asset.PropertyRepository;
import org.estatio.dom.asset.role.FixedAssetRoleTypeEnum;
import org.estatio.dom.party.PartyRoleTypeEnum;
import org.estatio.dom.party.Person;
import org.estatio.dom.party.PersonGenderType;
import org.estatio.dom.party.role.PartyRoleTypeService;
import org.estatio.fixture.EstatioFakeDataService;
import org.estatio.fixture.security.tenancy.ApplicationTenancyForGlobal;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(chain = true)
public class PersonBuilder extends PersonAbstract {

    @Getter @Setter
    private String atPath;

    @Getter @Setter
    private String reference;

    @Getter @Setter
    private String firstName;

    @Getter @Setter
    private String initials;

    @Getter @Setter
    private String lastName;

    @Getter @Setter
    private PersonGenderType personGenderType;

    @Getter @Setter
    private String phoneNumber;

    @Getter @Setter
    private String emailAddress;

    @Getter @Setter
    private String fromPartyStr;

    @Getter @Setter
    private String relationshipType;

    @Getter @Setter
    private String securityUsername;

    @Getter @Setter
    private String securityUserAccountCloneFrom;

    @Getter
    private List<PartyRoleTypeEnum> partyRoleTypes = Lists.newArrayList();
    public PersonBuilder addPartyRoleType(PartyRoleTypeEnum partyRoleType) {
        partyRoleTypes.add(partyRoleType);
        return this;
    }

    @Data
    static class FixedAssetRoleSpec {
        final FixedAssetRoleTypeEnum roleType;
        final String propertyRef;
    }

    @Getter
    private List<FixedAssetRoleSpec> fixedAssetRoles = Lists.newArrayList();
    public PersonBuilder addFixedAssetRole(
            final FixedAssetRoleTypeEnum fixedAssetRoleType,
            final String propertyRef) {
        fixedAssetRoles.add(new FixedAssetRoleSpec(fixedAssetRoleType, propertyRef));
        return this;
    }

    @Getter
    private Person person;


    @Override
    protected void execute(ExecutionContext executionContext) {

        defaultParam("atPath", executionContext, ApplicationTenancyForGlobal.PATH);
        defaultParam("reference", executionContext, fakeDataService.lorem().fixedString(6));
        defaultParam("firstName", executionContext, fakeDataService.name().firstName());
        defaultParam("lastName", executionContext, fakeDataService.name().fullName());
        defaultParam("personGenderType", executionContext, fakeDataService.collections().anEnum(PersonGenderType.class));
        defaultParam("initials", executionContext, firstName.substring(0,1));
        defaultParam("securityUserAccountCloneFrom", executionContext, "estatio-admin");

        person = createPerson(getAtPath(), getReference(), getInitials(), getFirstName(), getLastName(), getPersonGenderType(), getPhoneNumber(), getEmailAddress(), getFromPartyStr(), getRelationshipType(), executionContext);

        for (PartyRoleTypeEnum partyRoleType : partyRoleTypes) {
            partyRoleTypeService.createRole(person, partyRoleType);
        }

        for (FixedAssetRoleSpec spec : fixedAssetRoles) {
            Property property = propertyRepository.findPropertyByReference(spec.getPropertyRef());
            property.addRoleIfDoesNotExist(
                    person, spec.roleType, null, null);
            partyRoleTypeService.createRole(person, spec.roleType);
        }

        if(securityUsername != null) {
            ApplicationUser userToCloneFrom = applicationUserRepository.findByUsername(securityUserAccountCloneFrom);
            if(userToCloneFrom == null) {
                throw new IllegalArgumentException("Could not find any user with username: " + securityUserAccountCloneFrom);
            }

            applicationUserRepository.newLocalUserBasedOn(
                    securityUsername,
                    new Password("pass"), new Password("pass"),
                    userToCloneFrom, true, null);
            person.setUsername(securityUsername);
        }
    }

    @Inject
    EstatioFakeDataService fakeDataService;

    @Inject
    PartyRoleTypeService partyRoleTypeService;

    @Inject
    PropertyRepository propertyRepository;

    @Inject
    ApplicationUserRepository applicationUserRepository;

}

