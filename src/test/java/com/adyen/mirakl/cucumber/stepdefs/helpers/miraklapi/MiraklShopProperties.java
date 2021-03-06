/*
 *                       ######
 *                       ######
 * ############    ####( ######  #####. ######  ############   ############
 * #############  #####( ######  #####. ######  #############  #############
 *        ######  #####( ######  #####. ######  #####  ######  #####  ######
 * ###### ######  #####( ######  #####. ######  #####  #####   #####  ######
 * ###### ######  #####( ######  #####. ######  #####          #####  ######
 * #############  #############  #############  #############  #####  ######
 *  ############   ############  #############   ############  #####  ######
 *                                      ######
 *                               #############
 *                               ############
 *
 * Adyen Mirakl Connector
 *
 * Copyright (c) 2018 Adyen B.V.
 * This file is open source and available under the MIT license.
 * See the LICENSE file for more info.
 *
 */

package com.adyen.mirakl.cucumber.stepdefs.helpers.miraklapi;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Resource;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.adyen.mirakl.service.UboService;
import com.adyen.mirakl.startup.MiraklStartupValidator;
import com.google.common.collect.ImmutableList;
import com.mirakl.client.mmp.domain.common.currency.MiraklIsoCurrencyCode;
import com.mirakl.client.mmp.domain.shop.MiraklProfessionalInformation;
import com.mirakl.client.mmp.domain.shop.bank.MiraklAbaBankAccountInformation;
import com.mirakl.client.mmp.domain.shop.bank.MiraklIbanBankAccountInformation;
import com.mirakl.client.mmp.domain.shop.create.MiraklCreateShopAddress;
import com.mirakl.client.mmp.operator.domain.shop.create.MiraklCreateShop;
import com.mirakl.client.mmp.operator.domain.shop.create.MiraklCreateShopNewUser;
import com.mirakl.client.mmp.operator.domain.shop.create.MiraklCreatedShopReturn;
import com.mirakl.client.mmp.operator.domain.shop.create.MiraklCreatedShops;
import com.mirakl.client.mmp.request.additionalfield.MiraklRequestAdditionalFieldValue;

class MiraklShopProperties extends AbstractMiraklShopSharedProperties {

    @Resource
    private UboService uboService;

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private String companyName = FAKER.company().name();
    private String firstName = FAKER.name().firstName();

    void populatePaymentInformation(List<Map<String, String>> rows, MiraklCreateShop createShop) {
        rows.forEach(row -> {
            String owner;
            String bankName;
            String iban;
            String bic;
            String city;

            if (row.get("bank name") == null) {
                log.info("Bank account information will not be created in this test.");
            } else {
                owner = row.get("bankOwnerName");
                bankName = row.get("bank name");
                iban = row.get("iban");
                bic = FAKER.finance().bic();
                city = row.get("city");
                createShop.setPaymentInformation(miraklIbanBankAccountInformation(owner, bankName, iban, bic, city));
            }
        });
    }

    void populatePaymentInformationForUS(List<Map<String, String>> rows, MiraklCreateShop createShop) {
        rows.forEach(row -> {
            String owner;
            String bankName;
            String routingNumber;
            String bankAccountNumber;
            String city;
            String street;
            String zip;

            if (row.get("bank name") == null) {
                log.info("Bank account information will not be created in this test.");
            } else {
                owner = row.get("bankOwnerName");
                bankName = row.get("bank name");
                bankAccountNumber = row.get("bankAccountNumber");
                routingNumber = row.get("routingNumber");
                city = row.get("city");
                street = row.get("street");
                zip = row.get("zip");
                createShop.setPaymentInformation(miraklAbaBankAccountInformation(owner, bankName, routingNumber, bankAccountNumber, city, street, zip));
            }
        });

    }

    void populateShareholderInNonSequentialOrder(String legalEntity, List<Map<String, String>> rows, MiraklCreateShop createShop) {
        ImmutableList.Builder<MiraklRequestAdditionalFieldValue> builder = ImmutableList.builder();
        rows.forEach(row -> {
            maxUbos = row.get("UBO");
            int ubo = Integer.valueOf(maxUbos);
            Map<Integer, Map<String, String>> uboKeys = uboService.generateMiraklUboKeys(Integer.valueOf(maxUbos));
            buildShareHolderMinimumData(builder, ubo, uboKeys, civility());
            builder.add(createAdditionalField(MiraklStartupValidator.CustomMiraklFields.ADYEN_LEGAL_ENTITY_TYPE.toString(), legalEntity));
            builder.add(createAdditionalField(MiraklStartupValidator.CustomMiraklFields.ADYEN_BUSINESS_HOUSENUMBER.toString(), FAKER.address().streetAddressNumber()));
            createShop.setAdditionalFieldValues(builder.build());
        });
    }

    void populateShareHolderData(String legalEntity, List<Map<String, String>> rows, MiraklCreateShop createShop) {
        rows.forEach(row -> {
            maxUbos = row.get("maxUbos");
            if (maxUbos != null) {
                ImmutableList.Builder<MiraklRequestAdditionalFieldValue> builder = ImmutableList.builder();
                for (int i = 1; i <= Integer.valueOf(maxUbos); i++) {

                    Map<Integer, Map<String, String>> uboKeys = uboService.generateMiraklUboKeys(Integer.valueOf(maxUbos));
                    buildShareHolderMinimumData(builder, i, uboKeys, civility());
                    builder.add(createAdditionalField(uboKeys.get(i).get(UboService.COUNTRY), "GB"));
                    builder.add(createAdditionalField(uboKeys.get(i).get(UboService.HOUSE_NUMBER_OR_NAME), FAKER.address().streetAddressNumber()));
                    builder.add(createAdditionalField(uboKeys.get(i).get(UboService.STREET), FAKER.address().streetName()));
                    builder.add(createAdditionalField(uboKeys.get(i).get(UboService.CITY), FAKER.address().city()));
                    builder.add(createAdditionalField(uboKeys.get(i).get(UboService.POSTAL_CODE), FAKER.address().zipCode()));
                    builder.add(createAdditionalField(uboKeys.get(i).get(UboService.PHONE_COUNTRY_CODE), "GB"));
                    builder.add(createAdditionalField(uboKeys.get(i).get(UboService.PHONE_NUMBER), FAKER.phoneNumber().phoneNumber()));
                    builder.add(createAdditionalField(uboKeys.get(i).get(UboService.DATE_OF_BIRTH), dateOfBirth()));
                    builder.add(createAdditionalField(uboKeys.get(i).get(UboService.NATIONALITY), "GB"));
                    builder.add(createAdditionalField(uboKeys.get(i).get(UboService.ID_NUMBER), UUID.randomUUID().toString()));
                }
                builder.add(createAdditionalField(MiraklStartupValidator.CustomMiraklFields.ADYEN_LEGAL_ENTITY_TYPE.toString(), legalEntity));
                builder.add(createAdditionalField(MiraklStartupValidator.CustomMiraklFields.ADYEN_BUSINESS_HOUSENUMBER.toString(), FAKER.address().streetAddressNumber()));
                createShop.setAdditionalFieldValues(builder.build());
            }
        });
    }

    void populateShareHolderDataForNetherlands(String legalEntity, List<Map<String, String>> rows, MiraklCreateShop createShop) {
        rows.forEach(row -> {
            maxUbos = row.get("maxUbos");
            if (maxUbos != null) {
                ImmutableList.Builder<MiraklRequestAdditionalFieldValue> builder = ImmutableList.builder();
                for (int i = 1; i <= Integer.valueOf(maxUbos); i++) {

                    Map<Integer, Map<String, String>> uboKeys = uboService.generateMiraklUboKeys(Integer.valueOf(maxUbos));
                    buildShareHolderMinimumData(builder, i, uboKeys, civility());
                    builder.add(createAdditionalField(uboKeys.get(i).get(UboService.COUNTRY), "NL"));
                    builder.add(createAdditionalField(uboKeys.get(i).get(UboService.STREET), FAKERNL.address().streetName() + " " + FAKERNL.address().streetAddressNumber()));
                    builder.add(createAdditionalField(uboKeys.get(i).get(UboService.CITY), FAKERNL.address().city()));
                    builder.add(createAdditionalField(uboKeys.get(i).get(UboService.POSTAL_CODE), FAKERNL.address().zipCode()));
                    builder.add(createAdditionalField(uboKeys.get(i).get(UboService.PHONE_COUNTRY_CODE), "NL"));
                    builder.add(createAdditionalField(uboKeys.get(i).get(UboService.PHONE_NUMBER), FAKERNL.phoneNumber().phoneNumber()));
                    builder.add(createAdditionalField(uboKeys.get(i).get(UboService.DATE_OF_BIRTH), dateOfBirth()));
                    builder.add(createAdditionalField(uboKeys.get(i).get(UboService.NATIONALITY), "NL"));
                    builder.add(createAdditionalField(uboKeys.get(i).get(UboService.ID_NUMBER), UUID.randomUUID().toString()));
                }
                builder.add(createAdditionalField(MiraklStartupValidator.CustomMiraklFields.ADYEN_LEGAL_ENTITY_TYPE.toString(), legalEntity));
                builder.add(createAdditionalField(MiraklStartupValidator.CustomMiraklFields.ADYEN_BUSINESS_HOUSENUMBER.toString(), FAKER.address().streetAddressNumber()));
                createShop.setAdditionalFieldValues(builder.build());
            }
        });
    }

    void populateShareHolderDataForUS(String legalEntity, List<Map<String, String>> rows, MiraklCreateShop createShop) {
        rows.forEach(row -> {
            maxUbos = row.get("maxUbos");
            if (maxUbos != null) {
                ImmutableList.Builder<MiraklRequestAdditionalFieldValue> builder = ImmutableList.builder();
                for (int i = 1; i <= Integer.valueOf(maxUbos); i++) {

                    Map<Integer, Map<String, String>> uboKeys = uboService.generateMiraklUboKeys(Integer.valueOf(maxUbos));
                    buildShareHolderMinimumData(builder, i, uboKeys, civility());
                    builder.add(createAdditionalField(uboKeys.get(i).get(UboService.COUNTRY), "US"));
                    builder.add(createAdditionalField(uboKeys.get(i).get(UboService.STREET), FAKERUS.address().streetName() + " " + FAKERUS.address().streetAddressNumber()));
                    builder.add(createAdditionalField(uboKeys.get(i).get(UboService.CITY), "PASSED"));
                    builder.add(createAdditionalField(uboKeys.get(i).get(UboService.POSTAL_CODE), FAKERUS.address().zipCode().substring(0, 5)));
                    builder.add(createAdditionalField(uboKeys.get(i).get(UboService.PHONE_COUNTRY_CODE), "US"));
                    builder.add(createAdditionalField(uboKeys.get(i).get(UboService.PHONE_NUMBER), FAKERUS.phoneNumber().phoneNumber()));
                    builder.add(createAdditionalField(uboKeys.get(i).get(UboService.DATE_OF_BIRTH), dateOfBirth()));
                    builder.add(createAdditionalField(uboKeys.get(i).get(UboService.NATIONALITY), "US"));
                    builder.add(createAdditionalField(uboKeys.get(i).get(UboService.STATE_OR_PROVINCE), "CA"));
                    builder.add(createAdditionalField(uboKeys.get(i).get(UboService.ID_NUMBER), UUID.randomUUID().toString()));
                    builder.add(createAdditionalField(uboKeys.get(i).get(UboService.HOUSE_NUMBER_OR_NAME), FAKER.address().streetAddressNumber()));

                }
                builder.add(createAdditionalField(MiraklStartupValidator.CustomMiraklFields.ADYEN_LEGAL_ENTITY_TYPE.toString(), legalEntity));
                builder.add(createAdditionalField(MiraklStartupValidator.CustomMiraklFields.ADYEN_BUSINESS_HOUSENUMBER.toString(), FAKERUS.address().streetAddressNumber()));
                createShop.setAdditionalFieldValues(builder.build());


            }
        });
    }

    private String dateOfBirth() {
        return "1989-03-15T23:00:00Z";
    }

    void populateShareholderWithMissingData(String legalEntity, List<Map<String, String>> rows, MiraklCreateShop createShop) {
        rows.forEach(row -> {
            maxUbos = row.get("maxUbos");
            if (maxUbos != null) {
                ImmutableList.Builder<MiraklRequestAdditionalFieldValue> builder = ImmutableList.builder();
                Map<Integer, Map<String, String>> uboKeys = uboService.generateMiraklUboKeys(Integer.valueOf(maxUbos));

                for (int i = 1; i <= Integer.valueOf(maxUbos); i++) {
                    buildShareHolderMinimumData(builder, i, uboKeys, civility());
                }
                builder.add(createAdditionalField(MiraklStartupValidator.CustomMiraklFields.ADYEN_LEGAL_ENTITY_TYPE.toString(), legalEntity));
                createShop.setAdditionalFieldValues(builder.build());
            }
        });
    }

    private void buildShareHolderMinimumData(ImmutableList.Builder<MiraklRequestAdditionalFieldValue> builder, int i, Map<Integer, Map<String, String>> uboKeys, String civility) {
        String email = "adyen-mirakl-".concat(UUID.randomUUID().toString()).concat("@mailtrap.com");
        builder.add(createAdditionalField(uboKeys.get(i).get(UboService.CIVILITY), civility));
        builder.add(createAdditionalField(uboKeys.get(i).get(UboService.FIRSTNAME), FAKER.name().firstName()));
        builder.add(createAdditionalField(uboKeys.get(i).get(UboService.LASTNAME), FAKER.name().lastName()));
        builder.add(createAdditionalField(uboKeys.get(i).get(UboService.EMAIL), email));
    }

    void populateAddFieldsLegalAndHouseNumber(String legalEntity, MiraklCreateShop createShop) {

        createShop.setAdditionalFieldValues(ImmutableList.of(createAdditionalField(MiraklStartupValidator.CustomMiraklFields.ADYEN_INDIVIDUAL_HOUSENUMBER.toString(),
                                                                                   FAKER.address().streetAddressNumber()),
                                                             createAdditionalField(MiraklStartupValidator.CustomMiraklFields.ADYEN_LEGAL_ENTITY_TYPE.toString(), legalEntity),
                                                             createAdditionalField(MiraklStartupValidator.CustomMiraklFields.ADYEN_INDIVIDUAL_DOB.toString(), dateOfBirth()),
                                                             createAdditionalField(MiraklStartupValidator.CustomMiraklFields.ADYEN_INDIVIDUAL_IDNUMBER.toString(), "01234567890")));
    }

    void populateUserEmailAndShopName(MiraklCreateShop createShop, List<Map<String, String>> rows) {
        String shopName;
        if (rows.get(0).get("companyName") == null) {
            shopName = companyName.concat("-").concat(RandomStringUtils.randomAlphanumeric(8)).toLowerCase();
        } else {
            shopName = rows.get(0).get("companyName");
        }
        MiraklCreateShopNewUser newUser = new MiraklCreateShopNewUser();
        String email = "adyen-mirakl-".concat(UUID.randomUUID().toString()).concat("@mailtrap.com");
        newUser.setEmail(email);
        createShop.setNewUser(newUser);
        createShop.setEmail(email);
        if (rows.get(0).get("currency") != null) {
            createShop.setCurrencyIsoCode(MiraklIsoCurrencyCode.valueOf(rows.get(0).get("currency")));
        }
        log.info(String.format("\nShop name to create: [%s]", shopName));
        createShop.setName(shopName);
    }

    void populateMiraklProfessionalInformation(MiraklCreateShop createShop) {
        createShop.setProfessional(true);
        MiraklProfessionalInformation professionalInformation = new MiraklProfessionalInformation();
        professionalInformation.setCorporateName("TestData");
        professionalInformation.setIdentificationNumber(UUID.randomUUID().toString());
        createShop.setProfessionalInformation(professionalInformation);
    }

    void populateMiraklAddress(List<Map<String, String>> rows, MiraklCreateShop createShop) {
        rows.forEach(row -> {
            String city;

            if (row.get("city") == null || StringUtils.isEmpty(row.get("city"))) {
                city = FAKER.address().city();
            } else {
                city = row.get("city");
            }

            MiraklCreateShopAddress address = new MiraklCreateShopAddress();
            address.setCity(city);
            address.setCivility(civility());
            address.setCountry("GBR");
            address.setFirstname(firstName);
            address.setLastname(row.get("lastName"));
            address.setStreet1(FAKER.address().streetAddress());
            address.setZipCode(FAKER.address().zipCode());
            address.setState("Kent");
            createShop.setAddress(address);
        });
    }

    void populateMiraklAddressForNetherlands(MiraklCreateShop createShop) {
        MiraklCreateShopAddress address = new MiraklCreateShopAddress();
        address.setCity(FAKERNL.address().city());
        address.setCivility(civility());
        address.setCountry("NLD");
        address.setFirstname(FAKERNL.name().firstName());
        address.setLastname(FAKERNL.name().lastName());
        address.setStreet1(FAKERNL.address().streetAddress());
        address.setZipCode(FAKERNL.address().zipCode());
        address.setState(FAKERNL.address().state());
        createShop.setAddress(address);
    }

    void populateMiraklAddressForUS(MiraklCreateShop createShop) {
        MiraklCreateShopAddress address = new MiraklCreateShopAddress();
        address.setCity("PASSED");
        address.setCivility(civility());
        address.setCountry("USA");
        address.setState("CA");
        address.setFirstname(FAKERUS.name().firstName());
        address.setLastname(FAKERUS.name().lastName());
        address.setStreet1(FAKERUS.address().streetAddress());
        address.setZipCode(FAKERUS.address().zipCodeByState("CA"));
        createShop.setAddress(address);
    }


    void throwErrorIfShopIsNotCreated(MiraklCreatedShops shops) {
        MiraklCreatedShopReturn miraklCreatedShopReturn = shops.getShopReturns().stream().findAny().orElseThrow(() -> new IllegalStateException("No Shop found"));

        if (miraklCreatedShopReturn.getShopCreated() == null) {
            throw new IllegalStateException(miraklCreatedShopReturn.getShopError().getErrors().toString());
        }
        String shopId = shops.getShopReturns().iterator().next().getShopCreated().getId();
        log.info(String.format("Mirakl Shop Id: [%s]", shopId));
    }

    private MiraklIbanBankAccountInformation miraklIbanBankAccountInformation(String owner, String bankName, String iban, String bic, String city) {

        MiraklIbanBankAccountInformation miraklIbanBankAccountInformation = new MiraklIbanBankAccountInformation();
        miraklIbanBankAccountInformation.setOwner(owner);
        miraklIbanBankAccountInformation.setBankName(bankName);
        miraklIbanBankAccountInformation.setIban(iban);
        miraklIbanBankAccountInformation.setBic(bic);
        miraklIbanBankAccountInformation.setBankCity(city);
        return miraklIbanBankAccountInformation;
    }

    private MiraklAbaBankAccountInformation miraklAbaBankAccountInformation(String owner, String bankName, String routingNumber, String bankAccountNumber, String city, String street, String zip) {

        MiraklAbaBankAccountInformation miraklAbaBankAccountInformation = new MiraklAbaBankAccountInformation();
        miraklAbaBankAccountInformation.setOwner(owner);
        miraklAbaBankAccountInformation.setBankName(bankName);
        miraklAbaBankAccountInformation.setRoutingNumber(routingNumber);
        miraklAbaBankAccountInformation.setBankAccountNumber(bankAccountNumber);
        miraklAbaBankAccountInformation.setBankCity(city);
        miraklAbaBankAccountInformation.setBankStreet(street);
        miraklAbaBankAccountInformation.setBankZip(zip);
        return miraklAbaBankAccountInformation;
    }

}
