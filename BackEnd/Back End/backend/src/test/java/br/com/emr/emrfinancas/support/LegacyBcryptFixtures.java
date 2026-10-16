package br.com.emr.emrfinancas.support;

public final class LegacyBcryptFixtures {
 private LegacyBcryptFixtures() {}
 public static final String NORMAL = "SeguroAa1!xx";
 public static final String BOUNDARY = "Aa1!" + "x".repeat(68);
 public static final String UNICODE_73 = "Aa1!" + "\u00e9".repeat(34) + "A";
 public static final String HASH_2A = "$2a$08$JAM.aYG/oQaaCKPDaB5RF./nCUSH6GxDhRwRE7250HvE35kCt01LK";
 public static final String HASH_2Y = "$2y$12$gmYU44Gzq4RluDp7CgS8IOQudnOD2.zvkPUjUdQaYadqJXj5kjSxm";
 public static final String HASH_2B = "$2b$10$9bHB8FhbA/ch5cJ0NIzpRO6PrDdeJq/4Fu1q1/OCtbGmaDjU0v63y";
 public static final String BOUNDARY_HASH = "$2a$10$ZGFchqkdBkGZL2KamYU/1eVeLhoeVxL1HpAkV7AnH9ey/QMgyRMEO";
 public static final String UNICODE_LEGACY_HASH = "$2a$10$FvqlOVWwMbj7cutv1wG74elU3d2l5DitGIoVtJMTUTXriHwL7LN6C";
 public static final String COMPENSATION_HASH = "$2a$10$l7SxsX5dDp9fOVBY6jdDi.AaXkyqdQ2xUOuKehuGMhAdkRs2FdxWC";
}
