package se.fk.github.rtfmanuellkompletteringbff.model;

import se.fk.rimfrost.regel.rtf.manuell.komplettering.jaxrsspec.controllers.generatedsource.model.RtfKompletteringData;

/** Regeltjänstens GET-svar innehåller kompletteringsdata och informationsaccess-data (RTFKB-FR-05). */
public record BackendGetResponse(RtfKompletteringData data,BackendInformationsaccess informationsaccess){}
