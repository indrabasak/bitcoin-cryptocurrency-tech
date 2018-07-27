![](./img/scrooge.jpg)

Assignment 1: ScroogeCoin
=============================

In `ScroogeCoin`, the central authority Scrooge receives transactions from users. 

Scrooge organizes transactions into time periods or blocks. In each block, 

  * Scrooge will receive a list of transactions
  * Validate the transactions he receives
  * Publish a list of validated transactions
  
Points to remember:

  * A transaction can reference another in the same block. 
  * Among the transactions received by Scrooge in a single block, more than 
  one transaction may spend the same output. This is `double-spend` and hence 
  invalid. 
  * Transactions can’t be validated in isolation.

## List of Classes

|  Class                 | Function                                                                                         |
|------------------------|--------------------------------------------------------------------------------------------------|
| `Transaction.java`     | It represents a ScroogeCoin transaction and has inner classes `Transaction.Output` and `Transaction.Input`. A transaction consists of a `list of inputs`, a `list of outputs`, and a `unique ID`. <br><br> A `transaction output` consists of a value and a `public key` to which it is being paid. <br><br> A `transaction input` consists of a hash of the transaction that contains the corresponding output, the index of this output in that transaction (indices are simply integers starting from 0), and a digital signature. <br><br> For the input to be valid, the signature it contains must be a valid signature over the current transaction with the public key in the spent output. The raw data to be signed is obtained from the `getRawDataToSign` method.                                                                                                 |
| `Crypto.java`          | To verify a signature, the `verifySignature` method of `Crypto` class is used.                  |
| `UTXO.java`            | A `UTXO` class represents an `unspent transaction output`. An UTXO contains the hash of the transaction from which it originates and as well as the index within that transaction.                                     |
| `UTXOPool.java`        | The  `UTXOPool` class represents the current set of outstanding UTXOs and contains a map from each UTXO to its corresponding transaction output.                                                                              |
| `TxHandler.java`       | The `TxHandler` class does the following: <br><br> **Validates** a transaction by checking: <ol> <li>All outputs claimed by a transaction are in the current UTXO pool.</li>  <li>The signatures on each input of transaction are valid.</li> <li>No UTXO is claimed multiple times by the transaction.</li> <li>All of the transaction's output values are non-negative.</li> <li>The sum of transaction's input values is greater than or equal to the sum of its output values.</li></ol>  <br> **Processes** an unordered array of proposed transactions. Checking each transaction for correctness and returns a mutually valid array of accepted transactions. Updates the current UTXO pool as appropriate.                               |
| `MaxFeeTxHandler.java` | Finds a set of transactions with the maximum total transaction fees, i.e., maximum total sum over all the transactions in the set of the provided transactions. **(Extra Credit)**                                           |

## Submission Result
![](./img/assgnmt1-results.png)
