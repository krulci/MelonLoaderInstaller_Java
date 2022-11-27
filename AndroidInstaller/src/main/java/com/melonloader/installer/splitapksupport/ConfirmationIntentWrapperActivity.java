package com.melonloader.installer.splitapksupport;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class ConfirmationIntentWrapperActivity extends AppCompatActivity {

    private static final String EXTRA_CONFIRMATION_INTENT = "confirmation_intent";

    private static final int REQUEST_CODE_CONFIRM_INSTALLATION = 322;

    /**
     * Used to send abort event when this activity is force closed due to MainActivity being started from launcher/open with.
     * I'm not really sure why this works (Why isn't onActivityResult called on force close, also why this activity even gets destroyed when MainActivity is started), but if it doesn't, the only bad thing that will happen is the thing this prevents, so that's fine.
     */
    private boolean mFinishedProperly = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        Intent confirmationIntent = intent.getParcelableExtra(EXTRA_CONFIRMATION_INTENT);
        try {
            startActivityForResult(confirmationIntent, REQUEST_CODE_CONFIRM_INSTALLATION);
        } catch (Exception e) {
            Log.e("melonloader", e.getMessage());
            sendErrorBroadcast(intent.getIntExtra(SplitAPKService.EXTRA_SESSION_ID, -1), "Your ROM is incompatible with rootless installer. Try selecting another installer in SAI settings.");
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_CONFIRM_INSTALLATION) {
            mFinishedProperly = true;
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (!mFinishedProperly) {
            Intent intent = getIntent();
            sendErrorBroadcast(intent.getIntExtra(SplitAPKService.EXTRA_SESSION_ID, -1), "Installation was cancelled by user");
        }
    }

    public static void start(Context c, int sessionId, Intent confirmationIntent) {
        Intent intent = new Intent(c, ConfirmationIntentWrapperActivity.class);
        intent.putExtra(EXTRA_CONFIRMATION_INTENT, confirmationIntent);
        intent.putExtra(SplitAPKService.EXTRA_SESSION_ID, sessionId);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        c.startActivity(intent);
    }

    private void sendErrorBroadcast(int sessionID, String error) {
        Intent statusIntent = new Intent(SplitAPKService.ACTION_INSTALLATION_STATUS_NOTIFICATION);
        statusIntent.putExtra(SplitAPKService.EXTRA_INSTALLATION_STATUS, SplitAPKService.STATUS_FAILURE);
        statusIntent.putExtra(SplitAPKService.EXTRA_SESSION_ID, sessionID);
        statusIntent.putExtra(SplitAPKService.EXTRA_ERROR_DESCRIPTION, error);

        sendBroadcast(statusIntent);
    }

}