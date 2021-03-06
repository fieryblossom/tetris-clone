package Tetris;

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Game {
    private long window;

    private int vertexArrayID;
    private int programID;

    private int vertexBuffer;
    private FloatBuffer buffer;

    private int vertices;
    private int lineVertices;

    private GLFWErrorCallback errorCallback;

    private boolean restart;

    private final double targetTime = 1000 / 30;

    private double movementPeriod;
    private double lastMovement;
    private final double increaseRatio = 0.02;
    private double lastPeriod;

    private boolean dropped;

    private Block[][] matrix;
    private Piece piece;
    private Piece next;
    private IntQueue queue;
    private Piece held;
    private boolean canHold;

    private boolean[] lastKeyState = new boolean[7];
    private volatile boolean[] keyState = new boolean[7];

    private static final byte UP = 0;
    private static final byte DOWN = 1;
    private static final byte LEFT = 2;
    private static final byte RIGHT = 3;
    private static final byte SPACE = 4;
    private static final byte X = 5;
    private static final byte C = 6;

    private static final float scale = 0.09f; //static?
    private static final float translateX = -0.45f;
    private static final float translateY = -0.9f;
    private static final float border = 0.002f;

    private static final float[][] color = {
            {1f, 0f, 0f},
            {0f, 1f, 0f},
            {0f, 0f, 1f},
            {1f, 0f, 1f},
            {1f, 1f, 0f},
            {0f, 1f, 1f},
            {1f, 1f, 1f},
            {0.5f, 0.5f, 0.5f},
            {0f, 0f, 0f}
    };

    private Game(){
        init();
        do {
            //start screen
            setup();
            loop();
            //game over screen
        } while(restart);
        dispose();
    }
    private void init(){
        errorCallback = GLFWErrorCallback.createPrint();
        glfwSetErrorCallback(errorCallback);

        if(!glfwInit()){
            throw new IllegalStateException("Unable to initialize GLFW!");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_SAMPLES, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);

        window = glfwCreateWindow(600, 600, "Tetris", NULL, NULL);
        if(window==NULL){
            glfwTerminate();
            throw new RuntimeException("Failed to create the GLFW window!");
        }

        glfwMakeContextCurrent(window);
        GL.createCapabilities();

        glClearColor(0f, 0f, 0f, 0f);

        vertexArrayID = glGenVertexArrays();
        glBindVertexArray(vertexArrayID);

        programID = Shader.load("VertexShader.glsl", "FragmentShader.glsl");

        buffer = MemoryUtil.memAllocFloat(4096*2);

        vertexBuffer = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer);

        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 5*Float.BYTES, 0);

        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 5*Float.BYTES, 2*Float.BYTES);

        /*glfwSetKeyCallback(window, GLFWKeyCallback.create((window, key, scancode, action, mods) -> {
            if(key == GLFW_KEY_LEFT && (action == GLFW_PRESS || action == GLFW_REPEAT)) piece.moveLeft();
            else if(key == GLFW_KEY_RIGHT && (action == GLFW_PRESS || action == GLFW_REPEAT)) piece.moveRight();
            else if(key == GLFW_KEY_UP && action == GLFW_PRESS) piece.aRotate();
            else if(key == GLFW_KEY_X && action == GLFW_PRESS) piece.rotate();
            else if(key == GLFW_KEY_DOWN && action == GLFW_PRESS) startFastFwd();
            else if(key == GLFW_KEY_DOWN && action == GLFW_RELEASE) stopFastFwd();
            else if(key == GLFW_KEY_C && action == GLFW_PRESS) hold();
            else if(key == GLFW_KEY_SPACE && action == GLFW_PRESS) {
                piece.hardDrop();
                dropped = true;
            }
            else if(key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) System.exit(0);
        }));*/

        glfwSetKeyCallback(window, GLFWKeyCallback.create((window, key, scancode, action, mods) -> {
            if(key == GLFW_KEY_LEFT && (action == GLFW_PRESS || action == GLFW_REPEAT)) keyState[LEFT] = true;
            else if(key == GLFW_KEY_RIGHT && (action == GLFW_PRESS || action == GLFW_REPEAT)) keyState[RIGHT] = true;
            else if(key == GLFW_KEY_UP && action == GLFW_PRESS) keyState[UP] = true;
            else if(key == GLFW_KEY_X && action == GLFW_PRESS) keyState[X] = true;
            else if(key == GLFW_KEY_DOWN && action == GLFW_PRESS) startFastFwd();
            else if(key == GLFW_KEY_DOWN && action == GLFW_RELEASE) stopFastFwd();
            else if(key == GLFW_KEY_C && action == GLFW_PRESS) keyState[C] = true;
            else if(key == GLFW_KEY_SPACE && action == GLFW_PRESS) keyState[SPACE] = true;
            else if(key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) System.exit(0);
        }));
    }
    private void setup(){
        if(matrix == null) {
            matrix = new Block[20][10];
            for (int i = 0; i < 20; i++)
                for(int j = 0; j < 10; j++) {
                    matrix[i][j] = new Block();
                }
        } else
            for (int i = 0; i < 20; i++)
                for(int j = 0; j < 10; j++)
                    matrix[i][j].show = false;

        if(piece == null) piece = new Piece(matrix);
        if(next == null) next = new Piece(matrix);
        if(queue ==  null) queue = new IntQueue();
        else queue.reset();

        piece.set((byte) queue.next());
        next.set((byte) queue.next());

        movementPeriod = 1000;

        dropped = false;
        canHold = true;

        restart = false;
    }
    private void loop(){
        try{Thread.sleep(1000);} catch (InterruptedException ignored){} //prep time

        lastMovement = System.currentTimeMillis();

        while(true){
            double start = System.currentTimeMillis();

            handleInput();
            update();

            if(piece.collides()) break;

            putMatrix();
            putPiece();
            putHeld();
            putNext();
            putGhost();

            putGrid();
            putBlackGrid();

            render();

            glfwSwapBuffers(window);
            glfwPollEvents();

            if(glfwWindowShouldClose(window)) System.exit(0);

            double now = System.currentTimeMillis();
            double elapsed = now - start;
            Game.wait(targetTime - elapsed, now);
        }

        try{Thread.sleep(1000);} catch (InterruptedException ignored){} //finish time
    }
    private void dispose(){
        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);

        glDeleteBuffers(vertexBuffer);
        glDeleteProgram(programID);
        glDeleteVertexArrays(vertexArrayID);

        glfwTerminate();
    }
    private void handleInput(){
        for(byte i = 0; i < 7; i++) {
            lastKeyState[i] = keyState[i];
            keyState[i] = false;
        }

        if(lastKeyState[LEFT]) piece.moveLeft();
        else if(lastKeyState[RIGHT]) piece.moveRight();
        else if(lastKeyState[UP]) piece.aRotate();
        else if(lastKeyState[X]) piece.rotate();
        //else if(key == GLFW_KEY_DOWN && action == GLFW_PRESS) startFastFwd();
        //else if(key == GLFW_KEY_DOWN && action == GLFW_RELEASE) stopFastFwd();
        else if(lastKeyState[C]) hold();
        else if(lastKeyState[SPACE]) {
            piece.hardDrop();
            dropped = true;
        }
    }
    private void update(){
        double now = System.currentTimeMillis();

        if(now - lastMovement >= movementPeriod || dropped){
            dropped = false;
            lastMovement = now;
            if(piece.shouldStop()) {
                movementPeriod *= (1-increaseRatio);

                piece.place();

                Piece temp = piece;
                piece = next;
                next = temp;
                next.set((byte) queue.next());

                canHold = true;
            } else
                piece.moveDown();
        }

        purge();
    }
    private void purge(){
        for(byte i=0; i<20; i++)
            while(filled(i))
                removeLine(i);
    }
    private boolean filled(int i){
        for(int j = 0; j < 10; j++)
            if(!matrix[i][j].show) return false;
        return true;
    }
    private void removeLine(int i){
        Block[] temp = matrix[i];
        for(int k = i; k < 19; k++)
            matrix[k] = matrix[k+1];
        matrix[19] = temp;

        for(int j = 0; j < 10; j++)
            matrix[19][j].show = false;
    }
    private void startFastFwd(){
        lastPeriod = movementPeriod;
        movementPeriod = 25;
    }
    private void stopFastFwd(){
        movementPeriod = lastPeriod;
    }
    private void hold(){
        if(!canHold) return;
        if(held == null) {
            held = piece;
            piece = next;
            next = new Piece(matrix);
            next.set((byte) queue.next());
        } else{
            Piece temp = held;
            held = piece;
            piece = temp;
        }
        held.set(held.type); //reset x and y
        canHold = false;
    }
    private void putMatrix(){
        for(int y = 0; y < 20; y++)
            for(int x = 0; x < 10; x++){
                if(matrix[y][x].show) {
                    buffer.put(x * scale + translateX).put(y * scale + translateY)
                            .put(color[matrix[y][x].color]);
                    buffer.put((x + 1) * scale + translateX).put(y * scale + translateY)
                            .put(color[matrix[y][x].color]);
                    buffer.put((x + 1) * scale + translateX).put((y + 1) * scale + translateY)
                            .put(color[matrix[y][x].color]);

                    buffer.put(x * scale + translateX).put(y * scale + translateY)
                            .put(color[matrix[y][x].color]);
                    buffer.put((x + 1) * scale + translateX).put((y + 1) * scale + translateY)
                            .put(color[matrix[y][x].color]);
                    buffer.put(x * scale + translateX).put((y + 1) * scale + translateY)
                            .put(color[matrix[y][x].color]);

                    vertices += 6;
                }
            }
    }
    private void putPiece(){
        int x = piece.x, y = piece.y;
        for(int j = 0; j < 4; j++)
            for(int i = 0; i < 4; i++)
                if(piece.m[j][i]){
                    buffer.put((x + i) * scale + translateX).put((y + j) * scale + translateY)
                            .put(color[piece.type]);
                    buffer.put((x + i + 1) * scale + translateX).put((y + j) * scale + translateY)
                            .put(color[piece.type]);
                    buffer.put((x + i + 1) * scale + translateX).put((y + j + 1) * scale + translateY)
                            .put(color[piece.type]);

                    buffer.put((x + i) * scale + translateX).put((y + j) * scale + translateY)
                            .put(color[piece.type]);
                    buffer.put((x + i + 1) * scale + translateX).put((y + j + 1) * scale + translateY)
                            .put(color[piece.type]);
                    buffer.put((x + i) * scale + translateX).put((y + j + 1) * scale + translateY)
                            .put(color[piece.type]);

                    vertices += 6;
                }
    }
    private void putGhost(){
        int x = piece.x, y = piece.getGhostY();
        for(int j = 0; j < 4; j++)
            for(int i = 0; i < 4; i++)
                if(piece.m[j][i]){
                    buffer.put((x + i) * scale + translateX + border).put((y + j) * scale + translateY + border)
                            .put(color[piece.type]);
                    buffer.put((x + i + 1) * scale + translateX - border).put((y + j) * scale + translateY + border)
                            .put(color[piece.type]);
                    buffer.put((x + i + 1) * scale + translateX - border).put((y + j) * scale + translateY + border)
                            .put(color[piece.type]);
                    buffer.put((x + i + 1) * scale + translateX - border).put((y + j + 1) * scale + translateY - border)
                            .put(color[piece.type]);
                    buffer.put((x + i + 1) * scale + translateX - border).put((y + j + 1) * scale + translateY - border)
                            .put(color[piece.type]);
                    buffer.put((x + i) * scale + translateX + border).put((y + j + 1) * scale + translateY - border)
                            .put(color[piece.type]);
                    buffer.put((x + i) * scale + translateX + border).put((y + j + 1) * scale + translateY - border)
                            .put(color[piece.type]);
                    buffer.put((x + i) * scale + translateX + border).put((y + j) * scale + translateY + border)
                            .put(color[piece.type]);

                    lineVertices += 8;
                }
    }
    private void putNext(){
        for(int j = 0; j < 4; j++)
            for(int i = 0; i < 4; i++)
                if(next.m[j][i]){
                    buffer.put((11 + i) * scale + translateX).put((16 + j) * scale + translateY)
                            .put(color[next.type]);
                    buffer.put((11 + i + 1) * scale + translateX).put((16 + j) * scale + translateY)
                            .put(color[next.type]);
                    buffer.put((11 + i + 1) * scale + translateX).put((16 + j + 1) * scale + translateY)
                            .put(color[next.type]);

                    buffer.put((11 + i) * scale + translateX).put((16 + j) * scale + translateY)
                            .put(color[next.type]);
                    buffer.put((11 + i + 1) * scale + translateX).put((16 + j + 1) * scale + translateY)
                            .put(color[next.type]);
                    buffer.put((11 + i) * scale + translateX).put((16 + j + 1) * scale + translateY)
                            .put(color[next.type]);

                    vertices += 6;
                }
    }
    private void putHeld(){
        if(held == null) return;
        for(int j = 0; j < 4; j++)
            for(int i = 0; i < 4; i++)
                if(held.m[j][i]){
                    buffer.put((-5 + i) * scale + translateX).put((16 + j) * scale + translateY)
                            .put(color[held.type]);
                    buffer.put((-5 + i + 1) * scale + translateX).put((16 + j) * scale + translateY)
                            .put(color[held.type]);
                    buffer.put((-5 + i + 1) * scale + translateX).put((16 + j + 1) * scale + translateY)
                            .put(color[held.type]);

                    buffer.put((-5 + i) * scale + translateX).put((16 + j) * scale + translateY)
                            .put(color[held.type]);
                    buffer.put((-5 + i + 1) * scale + translateX).put((16 + j + 1) * scale + translateY)
                            .put(color[held.type]);
                    buffer.put((-5 + i) * scale + translateX).put((16 + j + 1) * scale + translateY)
                            .put(color[held.type]);

                    vertices += 6;
                }
    }
    private void putGrid(){
        for(int x = 0; x <= 10; x++) {
            buffer.put(x * scale + translateX).put(0 * scale + translateY).put(color[7]);
            buffer.put(x * scale + translateX).put(20 * scale + translateY).put(color[7]);

            lineVertices += 2;
        }
        for(int y = 0; y <= 20; y++) {
            buffer.put(0 * scale + translateX).put(y * scale + translateY).put(color[7]);
            buffer.put(10 * scale + translateX).put(y * scale + translateY).put(color[7]);

            lineVertices += 2;
        }
    }
    private void putBlackGrid(){
        //held
        for(int x = -5; x <= -1; x++) {
            buffer.put(x * scale + translateX).put(16 * scale + translateY).put(color[8]);
            buffer.put(x * scale + translateX).put(20 * scale + translateY).put(color[8]);

            lineVertices += 2;
        }
        for(int y = 16; y <= 20; y++) {
            buffer.put(-5 * scale + translateX).put(y * scale + translateY).put(color[8]);
            buffer.put(-1 * scale + translateX).put(y * scale + translateY).put(color[8]);

            lineVertices += 2;
        }
        //next
        for(int x = 11; x <= 15; x++) {
            buffer.put(x * scale + translateX).put(16 * scale + translateY).put(color[8]);
            buffer.put(x * scale + translateX).put(20 * scale + translateY).put(color[8]);

            lineVertices += 2;
        }
        for(int y = 16; y <= 20; y++) {
            buffer.put(11 * scale + translateX).put(y * scale + translateY).put(color[8]);
            buffer.put(15 * scale + translateX).put(y * scale + translateY).put(color[8]);

            lineVertices += 2;
        }
    }
    private void render(){
        buffer.flip();

        glClear(GL_COLOR_BUFFER_BIT);

        glUseProgram(programID);

        glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer);
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);

        glDrawArrays(GL_TRIANGLES, 0, vertices);

        glDrawArrays(GL_LINES, vertices, lineVertices);

        buffer.clear();
        vertices = 0;
        lineVertices = 0;
    }
    private static void wait(double time, double start){
        while(System.currentTimeMillis() - start < time){
            try{ Thread.sleep(1); } catch (InterruptedException ignored) {}
        }
    }
    private void startScreen(){

    }
    public static void main(String[] args){
        new Game();
    }
}
