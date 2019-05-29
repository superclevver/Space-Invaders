package com.mygdx.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Pools;
import com.badlogic.gdx.utils.TimeUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MyGdxGame extends ApplicationAdapter implements InputProcessor, Runnable
{
    private final float VELOCIDAD_ENEMIGO = -75f;
    private int NUMERO_VIDAS = 3;
    private int PUNTUACION = 0;

    private Music disparo;
    private Music explosion;
    private Music vidaPerdida;

    private int SCREEN_WIDTH;
    private int SCREEN_HEIGHT;

    private final float TOPE_PANTALLA = SCREEN_WIDTH - SCREEN_HEIGHT / 2;

    private SpriteBatch batch;
    private Texture textureNaveJugador, textureNaveEnemiga, textureDisparo, background, vida;
    private Sprite spriteNaveJugador;
    private ArrayList<Sprite> navesEnemigas;
    private ArrayList<Sprite> tiros;
    private ArrayList<Sprite> disparosEnemigos;
    private ArrayList<Sprite> numeroVidas;

    private int maxNavesEnemigas = 7;


    double lastShotPlayer = TimeUtils.millis();
    double lastShotEnemie = TimeUtils.millis();

    double shotFreqEnemies = 4000;
    double shotFreqPlayer = 980;

    BitmapFont font;

    @Override
    public void create ()
    {
        Gdx.input.setInputProcessor(this);

        SCREEN_WIDTH = Gdx.graphics.getWidth();
        SCREEN_HEIGHT = Gdx.graphics.getHeight();

        batch = new SpriteBatch();
        navesEnemigas = new ArrayList<Sprite>();

        textureNaveJugador = new Texture("ship_normal.png");
        textureNaveEnemiga = new Texture("black-bird.png");
        textureDisparo = new Texture("tret.png");
        background = new Texture("space.jpg");
        vida = new Texture("life.png");

        spriteNaveJugador = new Sprite(textureNaveJugador, 0, 0,
                textureNaveJugador.getWidth(), textureNaveJugador.getHeight());
        spriteNaveJugador.setX((SCREEN_WIDTH - spriteNaveJugador.getWidth()) / 2);
        spriteNaveJugador.setY(80);

        //Crear naves enemigas principal
        Thread t = new Thread(this);
        t.start();



        //Sonidos
        explosion = Gdx.audio.newMusic(Gdx.files.internal("explosion.mp3"));
        disparo = Gdx.audio.newMusic(Gdx.files.internal("disparo.mp3"));
        disparo.setVolume(5f);
        vidaPerdida = Gdx.audio.newMusic(Gdx.files.internal("shield_down.mp3"));
        vidaPerdida.setVolume(5f);

        tiros = new ArrayList<Sprite>();

        disparosEnemigos = new ArrayList<Sprite>();

        numeroVidas = new ArrayList<Sprite>();

        for (int i = 0; i < NUMERO_VIDAS; i++) {
            Sprite spriteVida = new Sprite(vida, 0,0, vida.getWidth(), vida.getHeight());
            spriteVida.setX((SCREEN_WIDTH-180)+i*60);
            spriteVida.setY(0);
            numeroVidas.add(spriteVida);
        }


        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("B612-Bold.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 12;
        font = generator.generateFont(parameter);
        generator.dispose();
    }

    @Override
    public void render ()
    {
        Gdx.gl.glClearColor(0, 0, 0, 0); // Color negro
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        batch.draw(background,0,0,SCREEN_WIDTH, SCREEN_HEIGHT);

        spriteNaveJugador.draw(batch);

        for(Sprite enemigo : navesEnemigas) {
            enemigo.draw(batch); //Dibujar la nave
            enemigo.translateY(Gdx.graphics.getDeltaTime() * VELOCIDAD_ENEMIGO ); //Mueve la nave hacia abajo
        }


        Iterator<Sprite> enemyIterator = navesEnemigas.iterator();
        if (navesEnemigas.size() != -1) {

            while (enemyIterator.hasNext()) {
                Sprite sprite = enemyIterator.next();
                if ( (sprite.getY() + sprite.getHeight()) < spriteNaveJugador.getY()) { //Si la nave enemiga llega por debajo de la posición de la nave del jugador
                    enemyIterator.remove(); // eliminamos la nave
                    Pools.free(sprite);
                    NUMERO_VIDAS--; // le quitamos una vida al jugador
                    vidaPerdida.play();
                    if(NUMERO_VIDAS >= 0) {
                        numeroVidas.remove(NUMERO_VIDAS);
                    }
                }


                Iterator<Sprite> tiroIterator = tiros.iterator();
                while(tiroIterator.hasNext()) {
                    Sprite tiro = tiroIterator.next();
                    if (Intersector.overlaps(sprite.getBoundingRectangle(), tiro.getBoundingRectangle())) { //si una nave enemiga y un disparo del jugador colisionan
                        enemyIterator.remove(); //eliminamos la nave
                        tiroIterator.remove(); //eliminamos el disparo
                        explosion.setVolume(4f); //volumen de la explosión a 4
                        explosion.play(); //creamos el sonido
                        PUNTUACION += 30;
                    }
                }

            }

        }

        //Movemos los disparos de la nave del jugador hacia arriba
        for(Sprite tiro : tiros) {
            tiro.draw(batch);
            tiro.translateY(Gdx.graphics.getDeltaTime() * 230f );
        }



        //Si no están todas las naves enemigas, creamos más
        /**
         * Cada 1500 milisegundos comprueba las naves enemigas en pantalla
         */
        if (navesEnemigas.size() < maxNavesEnemigas) {
            Runnable comprobarNavesEnemigas = new Runnable() {
                public void run() {
                    if (navesEnemigas.size() < maxNavesEnemigas) {
                        crearNavesEnemigas();
                    }
                }
            };

            ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
            executor.scheduleAtFixedRate(comprobarNavesEnemigas, 0, 4000, TimeUnit.MILLISECONDS);

        }



        if (TimeUtils.millis() - lastShotEnemie > shotFreqEnemies) {
            for(Sprite enemigo : navesEnemigas) {
                crearDisparoEnemigo(enemigo);
            }

            lastShotEnemie = TimeUtils.millis();
        }

        for (Sprite disparo : disparosEnemigos) {
            disparo.draw(batch);
            disparo.translateY(Gdx.graphics.getDeltaTime() * -230f );
        }





        Iterator<Sprite> disparosEnemigosIterator = disparosEnemigos.iterator();
        while(disparosEnemigosIterator.hasNext()) {
            Sprite disparo2 = disparosEnemigosIterator.next();
            if (Intersector.overlaps(spriteNaveJugador.getBoundingRectangle(), disparo2.getBoundingRectangle())) { //Si un disparo enemigo y la nave del jugador colisionan
                NUMERO_VIDAS--;
                vidaPerdida.play();

                if(NUMERO_VIDAS >= 0) {
                    numeroVidas.remove(NUMERO_VIDAS);
                }

                disparosEnemigosIterator.remove();
                Pools.free(disparo2);
            }
            else if (disparo2.getY() < spriteNaveJugador.getY()) {
                disparosEnemigosIterator.remove();
                Pools.free(disparo2);
            }
        }


        for(Sprite vidas : numeroVidas) {
            vidas.draw(batch);
        }

        if(NUMERO_VIDAS == -1) {
            Gdx.app.exit();
        }

        font.getData().setScale(2.5F);
        font.draw(batch, String.format("%06d", PUNTUACION), 0, 40);

        batch.end();
    }

    @Override
    public void dispose ()
    {
        batch.dispose();
        textureNaveJugador.dispose();
        textureNaveEnemiga.dispose();
        disparo.dispose();
        explosion.dispose();
        textureDisparo.dispose();
        vida.dispose();
    }


    @Override
    public boolean keyDown(int keycode)
    {
        float novaPos;

        switch(keycode)
        {
            case Input.Keys.DPAD_RIGHT:
                novaPos = spriteNaveJugador.getX() + 10;
                if(novaPos <= SCREEN_WIDTH - spriteNaveJugador.getWidth())
                {
                    spriteNaveJugador.setX(novaPos);
                }
                break;

            case Input.Keys.DPAD_LEFT:
                novaPos = spriteNaveJugador.getX() - 10;
                if(novaPos >= 0)
                {
                    spriteNaveJugador.setX(novaPos);
                }
                break;

            case Input.Keys.DPAD_UP:
                crearDisparo();
                break;

            case Input.Keys.DPAD_DOWN:

                break;
            case Input.Keys.SPACE:
                if (TimeUtils.millis() - lastShotPlayer > shotFreqPlayer) {
                    crearDisparo();
                    lastShotPlayer = TimeUtils.millis();
                }

                break;
        }

        return false;
    }

    @Override
    public boolean keyUp(int keycode)
    {
        return false;
    }

    @Override
    public boolean keyTyped(char character)
    {
        return false;
    }

    //Cuando el jugador toque y deje de tocar creará un disparo.
    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button)
    {
        if (TimeUtils.millis() - lastShotPlayer > shotFreqPlayer) {
            crearDisparo();
            lastShotPlayer = TimeUtils.millis();
        }
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button)
    {
        return false;
    }

    //Movemos la nave del jugador donde se arrastre el dedo por pantalla.
    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer)
    {
        spriteNaveJugador.setX(screenX-0.8f);
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY)
    {
        return false;
    }

    @Override
    public boolean scrolled(int amount)
    {
        return false;
    }

    @Override
    public void run()
    {
        crearNavesEnemigasInicio();
    }


    //Crea disparos apartir de la posición de la nave del jugador
    public void crearDisparo() {
        Sprite spriteDisparo = new Sprite(textureDisparo, 0, 0,
                textureDisparo.getWidth(), textureDisparo.getHeight());
        spriteDisparo.setX(spriteNaveJugador.getX());
        spriteDisparo.setY(spriteNaveJugador.getY());

        tiros.add(spriteDisparo);
        disparo.play();
    }

    //Crea tantas naves enemigas en una posición aleatoria como naves falten del total (7)
    public void crearNavesEnemigas() {

        for(int i = 0; i < maxNavesEnemigas - navesEnemigas.size(); i++) {
            Sprite spriteNaveEnemiga = new Sprite(textureNaveEnemiga, 0, 0,
                    textureNaveEnemiga.getWidth(), textureNaveEnemiga.getHeight());

            float x = MathUtils.random(0, SCREEN_WIDTH);
            float y = MathUtils.random(0, 80);

            spriteNaveEnemiga.setX(x);
            spriteNaveEnemiga.setY(SCREEN_HEIGHT-y-textureNaveEnemiga.getHeight());

            navesEnemigas.add(spriteNaveEnemiga);
        }
    }

    public void crearDisparoEnemigo(Sprite sprite) {
        Sprite spriteDisparo = new Sprite(textureDisparo, 0, 0,
                textureDisparo.getWidth(), textureDisparo.getHeight());
        spriteDisparo.setX(sprite.getX());
        spriteDisparo.setY(sprite.getY());

        disparosEnemigos.add(spriteDisparo);
    }

    public void crearNavesEnemigasInicio() {

        for(int i = 0; i < maxNavesEnemigas - navesEnemigas.size(); i++) {
            Sprite spriteNaveEnemiga = new Sprite(textureNaveEnemiga, 0, 0,
                    textureNaveEnemiga.getWidth(), textureNaveEnemiga.getHeight());

            float x = MathUtils.random(0, SCREEN_WIDTH);
            float y = MathUtils.random(0, 80);

            spriteNaveEnemiga.setX(x);
            spriteNaveEnemiga.setY(SCREEN_HEIGHT-y-textureNaveEnemiga.getHeight());

            navesEnemigas.add(spriteNaveEnemiga);
        }
    }



}
