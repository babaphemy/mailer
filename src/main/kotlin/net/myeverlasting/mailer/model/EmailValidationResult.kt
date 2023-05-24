package net.myeverlasting.mailer.model

data class EmailValidationResult(
    val result: Result
)

data class Result(
    val email: String,
    val verdict: String,
    val score: Double,
    val local: String,
    val host: String,
    val checks: Checks,
    val source: String,
    val ip_address: String
)

data class Checks(
    val domain: DomainChecks,
    val local_part: LocalPartChecks,
    val additional: AdditionalChecks
)

data class DomainChecks(
    val has_valid_address_syntax: Boolean,
    val has_mx_or_a_record: Boolean,
    val is_suspected_disposable_address: Boolean
)

data class LocalPartChecks(
    val is_suspected_role_address: Boolean
)

data class AdditionalChecks(
    val has_known_bounces: Boolean,
    val has_suspected_bounces: Boolean
)
