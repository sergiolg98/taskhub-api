package com.taskhub;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;

// Executable version of the hexagonal dependency rule: everything points towards the domain.
@AnalyzeClasses(packages = "com.taskhub", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domainDoesNotKnowFrameworksNorOtherLayers = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta..", "io.jsonwebtoken..",
                    "..application..", "..infrastructure..");

    // @Service and @Transactional (spring.stereotype / spring.transaction) are allowed on purpose:
    // they are annotations, not infrastructure. Data, web and security are not.
    @ArchTest
    static final ArchRule applicationDoesNotKnowInfrastructureDetails = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..infrastructure..", "org.springframework.data..", "org.springframework.web..",
                    "org.springframework.http..", "org.springframework.security..",
                    "jakarta.persistence..", "jakarta.servlet..", "io.jsonwebtoken..");

    @ArchTest
    static final ArchRule controllersTalkToPortsNotToPersistence = noClasses()
            .that().resideInAPackage("..infrastructure.web..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure.persistence..");

    @ArchTest
    static final ArchRule persistenceDoesNotKnowTheWeb = noClasses()
            .that().resideInAPackage("..infrastructure.persistence..")
            .should().dependOnClassesThat().resideInAnyPackage("..infrastructure.web..", "..infrastructure.security..");

    // Boundaries between business areas: the preparation for splitting them into services.
    @ArchTest
    static final ArchRule taskAreaOnlyKnowsAuthThroughItsLookupAdapter = noClasses()
            .that().resideInAPackage("com.taskhub.task..")
            .and().resideOutsideOfPackage("com.taskhub.task.infrastructure.lookup..")
            .should().dependOnClassesThat().resideInAPackage("com.taskhub.auth..");

    @ArchTest
    static final ArchRule authAreaDoesNotKnowTasks = noClasses()
            .that().resideInAPackage("com.taskhub.auth..")
            .should().dependOnClassesThat().resideInAPackage("com.taskhub.task..");

    // common holds what every service will need its own copy of (token validation, error format).
    @ArchTest
    static final ArchRule commonDoesNotKnowBusinessAreas = noClasses()
            .that().resideInAPackage("com.taskhub.common..")
            .should().dependOnClassesThat().resideInAnyPackage("com.taskhub.auth..", "com.taskhub.task..");

    @ArchTest
    static final ArchRule dependenciesAreInjectedByConstructor = NO_CLASSES_SHOULD_USE_FIELD_INJECTION;
}
