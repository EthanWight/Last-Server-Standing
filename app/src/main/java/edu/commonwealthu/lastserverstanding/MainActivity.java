package edu.commonwealthu.lastserverstanding;

import android.os.Bundle;
import android.graphics.PointF;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

import edu.commonwealthu.lastserverstanding.game.GameEngine;
import edu.commonwealthu.lastserverstanding.model.towers.FirewallTower;
import edu.commonwealthu.lastserverstanding.model.enemies.DataCrawler;
import edu.commonwealthu.lastserverstanding.view.GameView;

public class MainActivity extends AppCompatActivity {

    private GameView gameView;
    private GameEngine gameEngine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create game engine
        gameEngine = new GameEngine();

        // Add test content (optional - for testing)
        addTestContent();

        // Create game view
        gameView = new GameView(this);
        gameView.setGameEngine(gameEngine);

        // Set the game view as content (no XML layout needed)
        setContentView(gameView);
    }

    /**
     * Add test towers and enemies to verify rendering works
     * Remove this method once you have proper tower placement UI
     */
    private void addTestContent() {
        // Add a test tower at grid position (3, 3)
        FirewallTower testTower = new FirewallTower(new PointF(200, 200));
        gameEngine.addTower(testTower);

        // Add another tower
        FirewallTower testTower2 = new FirewallTower(new PointF(400, 300));
        gameEngine.addTower(testTower2);

        // Add test enemy with a simple path (left to right)
        List<PointF> path = new ArrayList<>();
        path.add(new PointF(50, 150));
        path.add(new PointF(300, 150));
        path.add(new PointF(300, 400));
        path.add(new PointF(700, 400));

        DataCrawler testEnemy = new DataCrawler(path);
        gameEngine.addEnemy(testEnemy);

        // Add a second enemy with slight delay (different starting position)
        List<PointF> path2 = new ArrayList<>();
        path2.add(new PointF(50, 250));
        path2.add(new PointF(400, 250));
        path2.add(new PointF(400, 300));
        path2.add(new PointF(800, 300));

        DataCrawler testEnemy2 = new DataCrawler(path2);
        gameEngine.addEnemy(testEnemy2);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (gameView != null) {
            gameView.stopGameLoop();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameView != null) {
            gameView.startGameLoop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gameView != null) {
            gameView.stopGameLoop();
        }
    }
}