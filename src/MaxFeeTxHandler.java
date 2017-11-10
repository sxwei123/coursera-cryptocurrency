import java.util.*;

/**
 * Created by leon on 9/11/17.
 */
public class MaxFeeTxHandler {

    private UTXOPool currentUTXOPool;
    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public MaxFeeTxHandler(UTXOPool utxoPool) {
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
        if(inputValueSum >= outputValueSum){
            //put Fees for this tx into hashmap
            if(!transactionFees.containsKey(tx)){
                transactionFees.put(tx, inputValueSum-outputValueSum);
            }
            return true;
        }
        return false;

    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        findNextValidTx(possibleTxs);
        return currentMaxFeesTxs;
    }

    private Stack<Transaction> tempTxs = new Stack<>();
    private double maxTxFee = 0;
    private Transaction[] currentMaxFeesTxs;
    private HashMap<Transaction, Double> transactionFees = new HashMap<>();
    //all removed utxo should be registered here
    private HashMap<UTXO, Transaction.Output> removedUtxos = new HashMap<>();

    private void findNextValidTx(Transaction[] possibleTxs){
        for(int i=0; i< possibleTxs.length; i++){
            Transaction ptx = possibleTxs[i];
            if(removeTxFromUTXOPoolIfValid(ptx)){
                //valid transaction's outputs can be the input of current or next transactions, add ptx's outputs to utxoPool
                for(int j=0; j<ptx.numOutputs(); j++){
                    UTXO utxo = new UTXO(ptx.getHash(), j);
                    currentUTXOPool.addUTXO(utxo, ptx.getOutput(j));
                }
                //put ptx into stack
                tempTxs.push(ptx);
                findNextValidTx(Arrays.copyOfRange(possibleTxs, i+1, possibleTxs.length));
                return;
            }
        }

        //code reaches here means no valid tx found, that it to say we reach the end of current route

        //calculate transaction fees
        Transaction[] currentValidTxs = tempTxs.toArray(new Transaction[tempTxs.size()]);
        double currentValidTxsTotalFees = 0;
        for(Transaction cvt : currentValidTxs){
            currentValidTxsTotalFees += transactionFees.get(cvt);
        }
        //if this is the max fee tx set ever found, store it in currentMaxFeesTxs
        System.out.println("currentValideTotal:"+currentValidTxsTotalFees + ", max:" + maxTxFee);
        if(currentValidTxsTotalFees >= maxTxFee){
            maxTxFee = currentValidTxsTotalFees;
            currentMaxFeesTxs = currentValidTxs;
        }
        //if the stack is empty, which means the end of recursion
        if(tempTxs.empty())  return;

        // pop the last tx in the stack
        Transaction lastTxInStack = tempTxs.pop();
        // revert tx, put inputs back to utxo pool and remove outputs from utxo pool
        for(int i=0; i < lastTxInStack.numInputs(); i++){
            Transaction.Input input = lastTxInStack.getInput(i);
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
            currentUTXOPool.addUTXO(utxo, removedUtxos.get(utxo));
        }
        for(int j=0; j < lastTxInStack.numOutputs(); j++){
            UTXO utxo = new UTXO(lastTxInStack.getHash(), j);
            Transaction.Output corOutput = currentUTXOPool.getTxOutput(utxo);
            removedUtxos.put(utxo, corOutput);
            currentUTXOPool.removeUTXO(utxo);

        }
        findNextValidTx(possibleTxs);
        return;
    }

    private boolean removeTxFromUTXOPoolIfValid(Transaction tx){
        if(isValidTx(tx)){
            for(int i=0; i < tx.numInputs(); i++){
                Transaction.Input input = tx.getInput(i);
                UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
                Transaction.Output corOutput = currentUTXOPool.getTxOutput(utxo);
                removedUtxos.put(utxo, corOutput);
                currentUTXOPool.removeUTXO(utxo);
            }
            return true;
        }
        return false;
    }
}
