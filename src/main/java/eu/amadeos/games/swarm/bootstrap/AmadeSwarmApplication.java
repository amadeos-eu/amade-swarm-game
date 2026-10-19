package eu.amadeos.games.swarm.bootstrap;

import eu.amadeos.commons.app.AmadeApplication;
import eu.amadeos.commons.app.AppInfo;
import eu.amadeos.gameengine.audio.Mixer;
import eu.amadeos.games.swarm.ui.game.SwarmGame;
import eu.amadeos.games.swarm.ui.main.SwarmPresenter;
import eu.amadeos.games.swarm.ui.main.SwarmWindow;

public final class AmadeSwarmApplication {
    public static final AppInfo APP = new AppInfo("amade-swarm-game", "Swarm", "swarm");

    private AmadeSwarmApplication() {
    }

    /** The version from the jar's manifest; "dev" when run from the IDE. */
    public static String version() {
        String version = AmadeSwarmApplication.class.getPackage().getImplementationVersion();
        return version == null ? "dev" : version;
    }

    // --8<-- [start:main]
    public static void main(String[] args) {
        AmadeApplication.launch(APP, injector -> {
            Mixer mixer = injector.getInstance(Mixer.class);
            mixer.start();
            SwarmWindow window = injector.getInstance(SwarmWindow.class);
            SwarmPresenter presenter = injector.getInstance(SwarmPresenter.class);
            SwarmGame game = injector.getInstance(SwarmGame.class);
            window.bind(presenter);
            presenter.start();
            window.whenClosed(() -> {
                game.stop();
                mixer.close();
                System.exit(0);
            });
            window.setLocationByPlatform(true);
            window.setVisible(true);
            game.start(window.canvas());
        }, new SwarmModule());
    }
    // --8<-- [end:main]
}
