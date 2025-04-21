package gr17.noodleio.game.util;

import com.badlogic.gdx.Gdx;

public class Timer {

    private int delay;
    private int elapsed;
    private boolean running;

    public Timer(int delay, boolean running) {

        this.delay = delay;
        this.running = running;
    }

    public boolean tick() {
        if(running) {
            elapsed += Gdx.graphics.getDeltaTime() * 1000;
            if(elapsed > delay) {
                return true;
            }
        }
        return false;
    }

    public void start() {

        this.running = true;
    }

    public void stop() {

        this.running = false;
    }

    public void reset() {

        this.elapsed = 0;
    }

    public int getElapsed() {

        return this.elapsed;
    }

    public void setElapsed(int elapsed) {

        this.elapsed = elapsed;
    }

    public int getDelay() {

        return delay;
    }

    public void setDelay(int delay) {

        this.delay = delay;
    }

    public boolean isRunning() {

        return running;
    }

    public float percent() {

        return elapsed / (float) delay;
    }

}
