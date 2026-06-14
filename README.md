# ID-based connection samples

This repository contains Azure Functions for Java samples that use Microsoft Entra ID-based authentication instead of connection strings to work with Azure Key Vault / Managed HSM and Azure Service Bus.

The repository includes two Function Apps that can be built and run independently.

| Directory | Function App | Purpose | Encryption | Service Bus entities |
|---|---|---|---|---|
| `keyvault/` | `func4kv25` | Encrypts an HTTP request body with an Azure Key Vault key, sends it through Service Bus, and decrypts it. | `RSA_OAEP` | Topic `kvt1`, Subscription `s1`, Queue `kvq1` |
| `Managed_HSM/` | `func4mhsm25` | Encrypts an HTTP request body with an Azure Managed HSM key, sends it through Service Bus, and decrypts it. | `A256CBC` | Topic `mhsmt1`, Subscription `s1`, Queue `mhsmq1` |

## How It Works

Each sample contains two functions.

| Function | Trigger / Binding | Behavior |
|---|---|---|
| `http2sb` | HTTP POST trigger, Service Bus topic output | Encrypts the HTTP request body and sends the encrypted payload to a Service Bus topic. |
| `topic2Queue` | Service Bus topic trigger, Service Bus queue output | Receives the encrypted payload from a topic subscription, decrypts it, and writes the result to a queue. |

Authentication uses the Azure SDK `DefaultAzureCredential` and Azure Functions Service Bus ID-based connections. Application settings store the Key Vault / Managed HSM key ID and Service Bus namespace, not secrets or connection strings.

## Current Stack

| Item | Version / Setting |
|---|---|
| Java | 25 |
| Azure Functions runtime | v4 |
| Azure Functions Java library | 3.3.0 |
| Azure Functions Maven Plugin | 1.42.0 |
| Azure Identity | 1.18.3 |
| Azure Key Vault Keys | 4.11.0 |
| Azure Service Bus | 7.17.18 |
| Hosting plan | Flex Consumption |
| Runtime OS | Linux |

## Prerequisites

- JDK 25
- Apache Maven
- Azure Functions Core Tools v4
- Azure CLI
- Azure subscription permissions to create or configure Function Apps, Service Bus, Key Vault, and Managed HSM resources

## Azure Resources

Prepare the following resources and permissions when using existing Azure resources.

### Common Resources

- Azure Service Bus namespace
- Topic, subscription, and queue
  - `keyvault/`: Topic `kvt1`, Subscription `s1`, Queue `kvq1`
  - `Managed_HSM/`: Topic `mhsmt1`, Subscription `s1`, Queue `mhsmq1`
- Service Bus send and receive permissions for the Function App managed identity, or for the developer identity used during local execution
  - Topic output: `Azure Service Bus Data Sender`
  - Topic trigger: `Azure Service Bus Data Receiver`
  - Queue output: `Azure Service Bus Data Sender`

### Key Vault Sample

- Azure Key Vault
- RSA key
- Key operation permissions for the Function App managed identity, or for the developer identity used during local execution
  - Example RBAC role: `Key Vault Crypto User`

### Managed HSM Sample

- Azure Managed HSM
- AES key
- Key operation permissions for the Function App managed identity, or for the developer identity used during local execution
  - Example RBAC role: `Managed HSM Crypto User`

## Application Settings

Each Function App requires the following settings.

| Setting | Description |
|---|---|
| `KEY_ID` | Key identifier for the Key Vault or Managed HSM key. |
| `sbConnection__fullyQualifiedNamespace` | Fully qualified Service Bus namespace, for example `my-namespace.servicebus.windows.net`. |
| `SERVICE_BUS_TOPIC_NAME` | Service Bus topic name used by the `http2sb` topic output binding. Use `kvt1` for `keyvault/` or `mhsmt1` for `Managed_HSM/` unless you changed the resource names. |
| `SERVICE_BUS_TOPIC_SUBSCRIPTION_NAME` | Service Bus topic subscription name used by the `http2sb` topic output binding. The default sample value is `s1`. |

When using a user-assigned managed identity, add identity details to the Service Bus connection prefix as needed.

The `@ServiceBusTopicOutput` annotation uses `%SERVICE_BUS_TOPIC_NAME%` and `%SERVICE_BUS_TOPIC_SUBSCRIPTION_NAME%`. In Azure Functions, this percent-sign syntax is a binding expression that resolves app settings at runtime. It isn't Windows environment-variable expansion, and it works the same way on Linux. For local runs, the values come from `local.settings.json`; in Azure, they come from Function App application settings.

```json
{
  "sbConnection__fullyQualifiedNamespace": "my-namespace.servicebus.windows.net",
  "sbConnection__credential": "managedidentity",
  "sbConnection__clientId": "<user-assigned-managed-identity-client-id>"
}
```

For local execution, create `local.settings.json` in the target sample directory. This file is excluded by `.gitignore`.

```json
{
  "IsEncrypted": false,
  "Values": {
    "AzureWebJobsStorage": "UseDevelopmentStorage=true",
    "FUNCTIONS_WORKER_RUNTIME": "java",
    "KEY_ID": "https://<vault-or-hsm-name>.<dns-suffix>/keys/<key-name>/<key-version>",
    "sbConnection__fullyQualifiedNamespace": "<service-bus-namespace>.servicebus.windows.net",
    "SERVICE_BUS_TOPIC_NAME": "<topic-name>",
    "SERVICE_BUS_TOPIC_SUBSCRIPTION_NAME": "s1"
  }
}
```

## Build

Build each sample as an independent Maven project.

```bash
cd keyvault
mvn clean package
```

```bash
cd Managed_HSM
mvn clean package
```

## Test

Run unit tests for each sample with Maven.

```bash
cd keyvault
mvn test
```

```bash
cd Managed_HSM
mvn test
```

The unit tests use test doubles for Azure Functions bindings and cryptography calls, so they do not require live Azure resources.

## Run Locally

Before running locally, sign in with an identity that can access Service Bus and Key Vault / Managed HSM.

```bash
az login
```

Then start Azure Functions from the target sample directory.

```bash
cd keyvault
mvn azure-functions:run
```

```bash
cd Managed_HSM
mvn azure-functions:run
```

Send an HTTP POST request to the endpoint. The request body is encrypted, sent to the topic, and then decrypted by `topic2Queue` into the queue output.

```bash
curl -i -X POST http://localhost:7071/api/http2sb \
  -H "Content-Type: text/plain" \
  --data "hello from id-based connection"
```

## Deploy

The following `pom.xml` properties define the default Function App name, resource group, region, and App Service plan for deployment.

| Directory | Function App | Resource Group | App Service Plan | Region |
|---|---|---|---|---|
| `keyvault/` | `func4kv25` | `keyvault` | `ASP-func4kv25` | `japaneast` |
| `Managed_HSM/` | `func4mhsm25` | `keyvault` | `ASP-func4mhsm25` | `japaneast` |

Update the values in `pom.xml` if needed, then deploy from the target sample directory.

```bash
mvn azure-functions:deploy
```

After deployment, enable the Function App managed identity and assign the required RBAC roles for Service Bus and Key Vault / Managed HSM. Finally, add `KEY_ID`, `sbConnection__fullyQualifiedNamespace`, `SERVICE_BUS_TOPIC_NAME`, and `SERVICE_BUS_TOPIC_SUBSCRIPTION_NAME` to the Function App application settings.

## Related Articles

| Topic | Original article | English article |
|---|---|---|
| Use Azure Key Vault and Service Bus with ID-based authentication | https://logico-jp.io/2022/04/01/id-base-authentication-for-azure-key-vault-and-service-bus/ | https://logicojp.medium.com/use-azure-key-vault-and-service-bus-with-id-based-authentication-c6556f88dcc2 |
| Use Managed HSM for encryption and decryption | https://logico-jp.io/2022/04/01/encryption-and-decryption-with-managed-hsm/ | https://medium.com/microsoftazure/use-managed-hsm-for-encryption-and-decryption-90b13c2af5fc |
