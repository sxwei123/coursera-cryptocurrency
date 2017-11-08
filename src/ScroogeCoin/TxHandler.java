package ScroogeCoin;

import java.util.ArrayList;
import java.util.List;

public class TxHandler {

    private UTXOPool currentUTXOPool;
    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
        currentUTXOPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        // IMPLEMENT THIS

        double inputValueSum = 0;
        double outputValueSum = 0;
        List<UTXO> utxosClaimed = new ArrayList<UTXO>();
        for(int i=0; i < tx.numInputs(); i++){
            Transaction.Input input = tx.getInput(i);
            //construct utxo for comparison
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
            // all outputs claimed by tx should be in the current UTXO pool
            if(!currentUTXOPool.contains(utxo)) return false;

            // get the unspent output(coin) from the utxoPool
            Transaction.Output prevOutput = currentUTXOPool.getTxOutput(utxo);
            // the signatures on each input of should be valid
            if(!Crypto.verifySignature(prevOutput.address, tx.getRawDataToSign(i), input.signature)) return false;

            //check if this UTXO already claimed
            if(utxosClaimed.contains(utxo)) return false;
            //put this utxo into claimed utxos
            utxosClaimed.add(utxo);
            inputValueSum += prevOutput.value;
        }

        for(Transaction.Output output: tx.getOutputs()){
            //all of tx output values are non-negative
            if(output.value < 0) return false;
            outputValueSum += output.value;
        }
        // the sum of {@code tx}s input values is greater than or equal to the sum of its output
        return inputValueSum >= outputValueSum;

    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        // mutually valid array of accepted transactions
        Transaction[] mvTxs = new Transaction[possibleTxs.length];
        int index = 0;
        for(Transaction ptx: possibleTxs){
            if(removeTxFromUTXOPoolIfValid(ptx)){
                mvTxs[index] = ptx;
                index++;
            }
        }

        return mvTxs;
    }

    private boolean removeTxFromUTXOPoolIfValid(Transaction tx){
        if(isValidTx(tx)){
            for(int i=0; i < tx.numInputs(); i++){
                Transaction.Input input = tx.getInput(i);
                UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
                currentUTXOPool.removeUTXO(utxo);
            }
            return true;
        }
        return false;
    }
}
