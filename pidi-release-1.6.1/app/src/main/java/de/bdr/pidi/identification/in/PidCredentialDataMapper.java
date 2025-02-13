/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.identification.in;

import de.bdr.pidi.authorization.out.identification.Address;
import de.bdr.pidi.authorization.out.identification.BirthPlace;
import de.bdr.pidi.authorization.out.identification.PidCredentialData;
import de.bdr.pidi.identification.core.model.Place;
import de.bdr.pidi.identification.core.model.ResponseData;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Mapper
public interface PidCredentialDataMapper {
    @Mapping(target = "pseudonym", source = "pseudonym")
    @Mapping(target = "nationality", source = "nationality")
    @Mapping(target = "givenName", source = "givenNames")
    @Mapping(target = "familyName", source = "familyNames")
    @Mapping(target = "birthdate", source = "dateOfBirth")
    @Mapping(target = "birthFamilyName", source = "birthName")
    @Mapping(target = "address", source = "residence")
    PidCredentialData map(ResponseData responseData);

    default LocalDate mapToLocalDate(String date ){
        return date == null ? null : LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    @Mapping(target = "country", ignore = true)
    @Mapping(target = "region", ignore = true)
    BirthPlace map(String locality);

    @Mapping(target = "formatted", source = "freeText")
    @Mapping(target = "country", source = "country")
    @Mapping(target = "streetAddress", source = "street")
    @Mapping(target = "postalCode", source = "zipCode")
    @Mapping(target = "locality", source = "city")
    @Mapping(target = "region", source = "state")
    Address map(Place place);
}
