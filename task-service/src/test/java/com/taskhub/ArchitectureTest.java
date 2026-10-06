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
                    "org.springframework..", "jakarta..", "io.jsonwebtoken..", "feign..", "io.github.resilience4j..",
                    "..application..", "..infrastructure..");

    // @Service and @Transactional (spring.stereotype / spring.transaction) are allowed on purpose:
    // they are annotations, not infrastructure. Data, web and security are not.
    @ArchTest
    static final ArchRule applicationDoesNotKnowInfrastructureDetails = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..infrastructure..", "org.springframework.data..", "org.springframework.web..",
                    "org.springframework.http..", "org.springframework.security..",
                    "jakarta.persistence..", "jakarta.servlet..", "io.jsonwebtoken..",
                    "org.springframework.cloud..", "feign..", "io.github.resilience4j..");

    @ArchTest
    static final ArchRule controllersTalkToPortsNotToPersistence = noClasses()
            .that().resideInAPackage("..infrastructure.web..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure.persistence..");

    @ArchTest
    static final ArchRule persistenceDoesNotKnowTheWeb = noClasses()
            .that().resideInAPackage("..infrastructure.persistence..")
            .should().dependOnClassesThat().resideInAnyPackage("..infrastructure.web..", "..infrastructure.security..");

    // common holds what every service will need its own copy of (token validation, error format).
    @ArchTest
    static final ArchRule commonDoesNotKnowBusinessAreas = noClasses()
            .that().resideInAPackage("com.taskhub.common..")
            .should().dependOnClassesThat().resideInAnyPackage("com.taskhub.task..");

    // task-service must not know the auth-service code: they only share the JWT contract (claims), never classes.
    @ArchTest
    static final ArchRule nothingKnowsTheAuthService = noClasses()
            .should().dependOnClassesThat().resideInAPackage("com.taskhub.auth..");

    // The HTTP client and its resilience are infrastructure details: only the lookup adapter's package may know them.
    @ArchTest
    static final ArchRule onlyTheLookupPackageKnowsFeignAndResilience4j = noClasses()
            .that().resideOutsideOfPackage("com.taskhub.task.infrastructure.lookup..")
            .should().dependOnClassesThat().resideInAnyPackage("feign..", "org.springframework.cloud.openfeign..",
                    "io.github.resilience4j..");

    @ArchTest
    static final ArchRule dependenciesAreInjectedByConstructor = NO_CLASSES_SHOULD_USE_FIELD_INJECTION;
}
