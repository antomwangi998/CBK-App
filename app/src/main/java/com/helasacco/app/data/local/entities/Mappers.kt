package com.helasacco.app.data.local.entities

import com.helasacco.app.domain.model.*

// ── UserEntity ↔ User ─────────────────────────────────────────────────────────

fun UserEntity.toDomain() = User(
    id = id,
    username = username,
    role = UserRole.from(role),
    fullName = fullName,
    email = email,
    phone = phone,
    branchId = branchId,
    isActive = isActive == 1,
    isLocked = isLocked == 1,
    lastLogin = lastLogin,
    memberId = memberId,
)

// ── BranchEntity ↔ Branch ─────────────────────────────────────────────────────

fun BranchEntity.toDomain() = Branch(
    id = id,
    code = code,
    name = name,
    location = location,
    city = city,
    county = county,
    phone = phone,
    isActive = isActive == 1,
    isHeadOffice = isHeadOffice == 1,
)

// ── MemberEntity ↔ Member ─────────────────────────────────────────────────────

fun MemberEntity.toDomain() = Member(
    id = id,
    memberNo = memberNo,
    branchId = branchId,
    firstName = firstName,
    lastName = lastName,
    otherNames = otherNames,
    idNumber = idNumber,
    dateOfBirth = dateOfBirth,
    gender = gender,
    phone = phone,
    email = email,
    address = address,
    city = city,
    county = county,
    occupation = occupation,
    employer = employer,
    mpesaNumber = mpesaNumber,
    kycStatus = KycStatus.from(kycStatus),
    isActive = isActive == 1,
    membershipDate = membershipDate,
    profilePhotoPath = profilePhotoPath,
    createdAt = createdAt,
)

fun Member.toEntity(
    createdBy: String? = null,
    deviceId: String? = null,
) = MemberEntity(
    id = id,
    memberNo = memberNo,
    branchId = branchId,
    firstName = firstName,
    lastName = lastName,
    otherNames = otherNames,
    fullNameSearch = fullName.lowercase(),
    idNumber = idNumber,
    dateOfBirth = dateOfBirth,
    gender = gender,
    phone = phone,
    email = email,
    address = address,
    city = city,
    county = county,
    occupation = occupation,
    employer = employer,
    mpesaNumber = mpesaNumber,
    kycStatus = kycStatus.value,
    isActive = if (isActive) 1 else 0,
    membershipDate = membershipDate,
    profilePhotoPath = profilePhotoPath,
    createdBy = createdBy,
    createdAt = createdAt,
    deviceId = deviceId,
    syncStatus = "pending",
)

// ── AccountEntity ↔ Account ───────────────────────────────────────────────────

fun AccountEntity.toDomain() = Account(
    id = id,
    accountNo = accountNo,
    memberId = memberId,
    branchId = branchId,
    accountType = AccountType.from(accountType),
    currency = currency,
    status = status,
    balanceMinor = balanceMinor,
    availableBalanceMinor = availableBalanceMinor,
    interestRate = interestRate,
    openingDate = openingDate,
    lastTransactionDate = lastTransactionDate,
)

// ── TransactionEntity ↔ Transaction ──────────────────────────────────────────

fun TransactionEntity.toDomain() = Transaction(
    id = id,
    transactionNo = transactionNo,
    accountId = accountId,
    memberId = memberId,
    branchId = branchId,
    transactionType = TransactionType.from(transactionType),
    amountMinor = amountMinor,
    balanceAfterMinor = balanceAfterMinor,
    currency = currency,
    description = description,
    reference = reference,
    status = status,
    processedBy = processedBy,
    createdAt = createdAt,
    valueDate = valueDate,
)

// ── LoanEntity ↔ Loan ─────────────────────────────────────────────────────────

fun LoanEntity.toDomain() = Loan(
    id = id,
    loanNo = loanNo,
    memberId = memberId,
    accountId = accountId,
    branchId = branchId,
    productId = productId,
    principalMinor = principalMinor,
    outstandingMinor = outstandingMinor,
    interestRate = interestRate,
    termMonths = termMonths,
    status = LoanStatus.from(status),
    disbursementDate = disbursementDate,
    maturityDate = maturityDate,
    nextPaymentDate = nextPaymentDate,
    nextPaymentMinor = nextPaymentMinor,
    createdAt = createdAt,
)

// ── NotificationEntity ↔ Notification ────────────────────────────────────────

fun NotificationEntity.toDomain() = Notification(
    id = id,
    userId = userId,
    memberId = memberId,
    title = title,
    message = message,
    notificationType = notificationType,
    isRead = isRead == 1,
    priority = priority,
    createdAt = createdAt,
)
