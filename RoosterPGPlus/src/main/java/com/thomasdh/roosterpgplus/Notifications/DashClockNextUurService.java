package com.thomasdh.roosterpgplus.Notifications;

import android.content.Intent;

import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;
import com.thomasdh.roosterpgplus.Data.Rooster;
import com.thomasdh.roosterpgplus.R;
import com.thomasdh.roosterpgplus.RoosterActivity;

public class DashClockNextUurService extends DashClockExtension {
    @Override
    protected void onUpdateData(int reason) {
        Rooster.getNextLesuur(this, nextLes -> {
            if(nextLes == null) {
                publishUpdate(new ExtensionData()
                                .visible(true)
                                .icon(R.drawable.ic_notification)
                                .status("Geen volgende les bekend"));
                return;
            }
            publishUpdate(new ExtensionData()
                            .visible(true)
                            .icon(R.drawable.ic_notification)
                            .status("Volgende les")
                            .expandedTitle("Volgende les: " + nextLes.vak)
                            .expandedBody(Rooster.getNextLesuurText(nextLes))
                            .clickIntent(new Intent(this, RoosterActivity.class))
            );
        });
    }
}
