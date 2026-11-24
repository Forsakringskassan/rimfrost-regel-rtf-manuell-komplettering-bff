package se.fk.github.rtfmanuellkompletteringbff.model;

import java.util.UUID;

/** Informationsaccess-data från regeltjänstens GET-svar (RTFKB-FR-05). */
public record BackendInformationsaccess(UUID uppgiftId,UUID regelId,String regelVersion,BackendAnvandare anvandare){}
