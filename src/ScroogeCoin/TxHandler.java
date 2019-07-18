package ScroogeCoin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TxHandler {

    private UTXOPool currentUTXOPool;
    /**
     * Creates a public ledger whose current ScroogeCoin.UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the ScroogeCoin.UTXOPool(ScroogeCoin.UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
        currentUTXOPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by the inputs of {@code tx} are in the current ScroogeCoin.UTXO pool,
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no ScroogeCoin.UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        // IMPLEMENT THIS

        double inputValueSum = 0;
        double outputValueSum = 0;
        Set<UTXO> utxosClaimed = new HashSet<UTXO>();
        for(int i=0; i < tx.numInputs(); i++){
            Transaction.Input input = tx.getInput(i);
            //construct utxo for comparison
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
            // all outputs claimed by tx should be in the current ScroogeCoin.UTXO pool
            if(!currentUTXOPool.contains(utxo)) return false;

            // get the unspent output(coin) from the utxoPool
            Transaction.Output prevOutput = currentUTXOPool.getTxOutput(utxo);
            // the signatures on each input of should be valid
            if(!Crypto.verifySignature(prevOutput.address, tx.getRawDataToSign(i), input.signature)) return false;

            //check if this ScroogeCoin.UTXO already claimed
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
     * updating the current ScroogeCoin.UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        // mutually valid array of accepted transactions
        List<Transaction> mvTxs = new ArrayList<Transaction>();
        int index = 0;
        for(Transaction ptx: possibleTxs){
            if(isValidTx(ptx)){
                for(int i=0; i < ptx.numInputs(); i++){
                    Transaction.Input input = ptx.getInput(i);
                    UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
                    currentUTXOPool.removeUTXO(utxo);
                }
                mvTxs.add(ptx);
                index++;

                //valid transaction's outputs can be the input of current or next transactions, add ptx's outputs to utxoPool
                for(int i=0; i<ptx.numOutputs(); i++){
                    UTXO utxo = new UTXO(ptx.getHash(), i);
                    currentUTXOPool.addUTXO(utxo, ptx.getOutput(i));
                }
            }
        }

        return mvTxs.toArray(new Transaction[0]);
    }

}
