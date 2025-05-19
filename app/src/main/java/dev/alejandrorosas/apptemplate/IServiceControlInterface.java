package dev.alejandrorosas.apptemplate;

import android.content.Context;

public interface IServiceControlInterface {

    void stop(Context context);

    void start(Context context, String endpoint);
}
