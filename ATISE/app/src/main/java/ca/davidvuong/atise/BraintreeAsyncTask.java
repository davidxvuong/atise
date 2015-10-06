package ca.davidvuong.atise;

import android.os.AsyncTask;

import com.braintreegateway.*;
import java.math.BigDecimal;

/**
 * Created by David Vuong on 9/20/2015.
 */
public class BraintreeAsyncTask extends AsyncTask<Void, Void, Result<Transaction>> {
    private AsyncResponse callback = null;
    String creditCardNum;
    String expDate;
    String cvv;
    String amount;

    public BraintreeAsyncTask(AsyncResponse callback, String creditCardNum, String expDate, String cvv, String amount) {
        this.callback = callback;
        this.expDate = expDate;
        this.creditCardNum = creditCardNum;
        this.cvv = cvv;
        this.amount = amount;
    }

    @Override
    protected Result<Transaction> doInBackground(Void... params) {
        BraintreeGateway paymentInstance = new BraintreeGateway(
                Environment.SANDBOX,
                "fg925ysqr466sbgq",
                "m624n6qbknfhfkjf",
                "4059bbe37fe7fcdb74452d772ebb611a"
        );
        TransactionRequest request = new TransactionRequest().
                amount(new BigDecimal(amount)).
                creditCard().
                number(creditCardNum).
                expirationDate(expDate).
                cvv(cvv).
                done();

        Result<Transaction> result = paymentInstance.transaction().sale(request);

        return result;
    }

    @Override
    protected void onPostExecute(Result<Transaction> result) {
        String temp = amount;
        callback.processFinish(result.isSuccess());
    }
}
