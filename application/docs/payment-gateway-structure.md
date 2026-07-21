
# Payment Module Architecture Documentation

## Overview

The payment module is designed to support **multiple payment gateways** (Tabby, SSLCommerz, Stripe, etc.) without changing business logic.

The architecture follows several design patterns to keep the system:

* Open for new payment providers
* Closed for modification
* Easy to test
* Easy to maintain
* Independent of any payment provider

---

# Architecture Diagram

```text
                         PaymentController
                                │
                                ▼
                    PaymentFacadeService
                                │
                                ▼
                    PaymentGatewayFactory
                                │
          ┌─────────────────────┼─────────────────────┐
          ▼                     ▼                     ▼
    TabbyPaymentGateway   SslPaymentGateway   StripePaymentGateway
          │                     │                     │
          ▼                     ▼                     ▼
   TabbyPaymentService   SslPaymentService   StripePaymentService
          │                     │                     │
          ▼                     ▼                     ▼
      Tabby API          SSLCommerz API        Stripe API
```

---

# Flow

## Step 1

Client calls

```
POST /api/v1/payments/checkout
```

Controller only receives the request.

```
PaymentController
```

↓

Calls

```
PaymentFacadeService.checkout(...)
```

Controller never knows whether the payment is Tabby or Stripe.

---

## Step 2

Facade receives

```
CheckoutCommand
```

Example

```
paymentMethod = TABBY
```

↓

Facade asks

```
PaymentGatewayFactory
```

```
Which gateway should handle this request?
```

---

## Step 3

Factory returns

```
TabbyPaymentGateway
```

or

```
SslPaymentGateway
```

or

```
StripePaymentGateway
```

depending on

```
PaymentMethod
```

---

## Step 4

Gateway converts common DTO into provider DTO.

Example

Common

```
CheckoutCommand
```

↓

Tabby

```
TabbyCheckoutCommand
```

↓

Calls

```
TabbyPaymentService.initiateCheckout(...)
```

---

## Step 5

Tabby service communicates with Tabby API.

```
Application
        │
        ▼
TabbyPaymentService
        │
        ▼
TabbyApiClient
        │
        ▼
Tabby REST API
```

---

## Step 6

Tabby returns

```
checkoutUrl

paymentId

status
```

↓

Gateway converts provider response into

```
PaymentResult
```

↓

Facade returns

```
PaymentResult
```

↓

Controller returns JSON.

---

# Design Patterns Used

---

# 1. Facade Pattern

Class

```
PaymentFacadeService
```

Purpose

Provides one entry point for every payment gateway.

Without Facade

```
Controller

↓

if(Tabby)

↓

TabbyService

if(SSL)

↓

SSLService

if(Stripe)

↓

StripeService
```

Controller becomes ugly.

With Facade

```
Controller

↓

PaymentFacadeService

↓

Factory

↓

Gateway
```

Controller knows nothing about payment providers.

---

# 2. Factory Pattern

Class

```
PaymentGatewayFactory
```

Purpose

Returns the proper payment gateway.

Example

```
PaymentGateway gateway =
factory.getGateway(PaymentMethod.TABBY);
```

Instead of

```
if(tabby)

if(stripe)

if(ssl)
```

inside controller.

---

# 3. Strategy Pattern

Interface

```
PaymentGateway
```

Implementations

```
TabbyPaymentGateway

SslPaymentGateway

StripePaymentGateway
```

Every gateway follows the same contract.

```
checkout()

confirm()

refund()

webhook()
```

Business layer never cares which implementation is executing.

---

# 4. Adapter Pattern

Each gateway converts the common DTO into provider-specific DTO.

Example

```
CheckoutCommand
```

↓

```
TabbyCheckoutCommand
```

↓

```
Tabby API
```

or

```
CheckoutCommand
```

↓

```
StripeCheckoutRequest
```

↓

```
Stripe API
```

This isolates provider-specific models.

---

# 5. Single Responsibility Principle (SOLID)

Every class has one responsibility.

### PaymentController

Receives HTTP request.

---

### PaymentFacadeService

Coordinates payment flow.

---

### PaymentGatewayFactory

Returns gateway implementation.

---

### TabbyPaymentGateway

Converts DTOs and delegates to Tabby service.

---

### TabbyPaymentService

Contains all Tabby business logic.

---

### TabbyApiClient

Only performs HTTP communication.

---

### PaymentCompletionService

Creates

* Wallet Deposit
* Transaction
* Wallet Balance

after payment success.

---

# Request Flow

```
Client

↓

PaymentController

↓

PaymentFacadeService

↓

PaymentGatewayFactory

↓

TabbyPaymentGateway

↓

TabbyPaymentService

↓

TabbyApiClient

↓

Tabby
```

---

# Webhook Flow

Unlike checkout, webhooks originate from the payment provider.

```
Tabby

↓

POST /webhook/tabby

↓

TabbyWebhookController

↓

TabbyPaymentService

↓

Update Payment Status

↓

PaymentCompletionService

↓

Wallet Deposit

↓

Transaction

↓

Wallet Balance
```

Notice that the webhook **does not** go through `PaymentFacadeService` or `PaymentGatewayFactory`. Each payment provider has its own webhook payload format and signature verification requirements, so each gateway exposes its own dedicated webhook endpoint.

---

# Responsibilities

| Class                    | Responsibility                                               |
| ------------------------ | ------------------------------------------------------------ |
| PaymentController        | Receives payment requests                                    |
| PaymentFacadeService     | Single entry point for payment operations                    |
| PaymentGatewayFactory    | Returns appropriate gateway                                  |
| PaymentGateway           | Common contract for all gateways                             |
| TabbyPaymentGateway      | Maps common request to Tabby request                         |
| TabbyPaymentService      | Implements Tabby payment lifecycle                           |
| TabbyApiClient           | Calls Tabby REST APIs                                        |
| PaymentCompletionService | Finalizes successful payments (deposit, transaction, wallet) |

---

# How to Add a New Payment Gateway

Adding a new provider should not require changes to the controller or business flow.

### 1. Create a new service

```text
BkashPaymentService
```

---

### 2. Create a gateway

```text
BkashPaymentGateway
```

Implements

```
PaymentGateway
```

---

### 3. Add provider-specific DTOs

```
BkashCheckoutCommand

BkashPaymentResponse
```

---

### 4. Register the gateway

```
@Component
public class BkashPaymentGateway implements PaymentGateway
```

The factory discovers it automatically (or via a map of implementations, depending on your implementation).

---

### 5. Done

No changes are required in:

* `PaymentController`
* `PaymentFacadeService`
* Existing gateway implementations

The system simply routes requests based on the selected `PaymentMethod`, making the architecture extensible and compliant with the Open/Closed Principle.
