package Tetris;

import java.nio.FloatBuffer;

public class Piece {
    private int x1,x2,x3,x4;
    private int y1,y2,y3,y4;

    public byte color;

    private float cx, cy;

    public void set(byte type){
        color = type;

        if(type==0){
            x1 = 4; y1 = 18;
            x2 = 5; y2 = 18;
            x3 = 4; y3 = 19;
            x4 = 5; y4 = 19;

            cx = 4.5f; cy = 18.5f;
        } else if(type==1){
            x1 = 3; y1 = 19;
            x2 = 4; y2 = 19;
            x3 = 5; y3 = 19;
            x4 = 6; y4 = 19;

            cx = 5.0f; cy = 19.0f;
        } else if(type==2){
            x1 = 3; y1 = 18;
            x2 = 4; y2 = 18;
            x3 = 4; y3 = 19;
            x4 = 5; y4 = 18;

            cx = 4.0f; cy = 18.0f;
        } else if(type==3){
            x1 = 3; y1 = 18;
            x2 = 4; y2 = 18;
            x3 = 5; y3 = 18;
            x4 = 5; y4 = 19;

            cx = 4.0f; cy = 18.0f;
        } else if(type==4){
            x1 = 3; y1 = 19;
            x2 = 3; y2 = 18;
            x3 = 4; y3 = 18;
            x4 = 5; y4 = 18;

            cx = 4.0f; cy = 18.0f;
        } else if(type==5){
            x1 = 3; y1 = 18;
            x2 = 4; y2 = 18;
            x3 = 4; y3 = 19;
            x4 = 5; y4 = 19;

            cx = 4.0f; cy = 19.0f;
        } else if(type==6){
            x1 = 3; y1 = 19;
            x2 = 4; y2 = 19;
            x3 = 4; y3 = 18;
            x4 = 5; y4 = 18;

            cx = 4.0f; cy = 19.0f;
        }
    }
    public void rotate(){
        int x, y;

        x = x1; y = y1;
        x1 = -y+(int)(cy+cx); y1 = x-(int)(cx-cy);

        x = x2; y = y2;
        x2 = -y+(int)(cy+cx); y2 = x-(int)(cx-cy);

        x = x3; y = y3;
        x3 = -y+(int)(cy+cx); y3 = x-(int)(cx-cy);

        x = x4; y = y4;
        x4 = -y+(int)(cy+cx); y4 = x-(int)(cx-cy);
    }
    public void moveLeft(Block[][] matrix){
        int leftmost = x1;
        int y = y1;
        if(x2 < leftmost) {
            leftmost = x2;
            y = y2;
        }
        if(x3 < leftmost) {
            leftmost = x3;
            y = y3;
        }
        if(x4 < leftmost) {
            leftmost = x4;
            y = y4;
        }

        if(leftmost <= 0) return;
        if(!matrix[y][leftmost-1].show) {
            x1--;
            x2--;
            x3--;
            x4--;

            cx--;
        }
    }
    public void moveRight(Block[][] matrix){
        int rightmost = x1;
        int y = y1;
        if(x2 > rightmost) {
            rightmost = x2;
            y = y2;
        }
        if(x3 > rightmost) {
            rightmost = x3;
            y = y3;
        }
        if(x4 > rightmost) {
            rightmost = x4;
            y = y4;
        }

        if(rightmost >= 9) return;
        if(!matrix[y][rightmost+1].show) {
            x1++;
            x2++;
            x3++;
            x4++;

            cx++;
        }
    }
    public void hardDrop(Block[][] matrix){
        while(!stop(matrix)) moveDown();
    }
    public void moveDown(){
        y1--;
        y2--;
        y3--;
        y4--;

        cy--;
    }
    public boolean stop(Block[][] matrix){
        if(y1 == 0) return true;
        if(matrix[y1-1][x1].show) return true;

        if(y2 == 0) return true;
        if(matrix[y2-1][x2].show) return true;

        if(y3 == 0) return true;
        if(matrix[y3-1][x3].show) return true;

        if(y4 == 0) return true;
        if(matrix[y4-1][x4].show) return true;

        return false;
    }
    public void place(Block[][] matrix){
        matrix[y1][x1].show = true;
        matrix[y2][x2].show = true;
        matrix[y3][x3].show = true;
        matrix[y4][x4].show = true;

        matrix[y1][x1].setColor(color);
        matrix[y2][x2].setColor(color);
        matrix[y3][x3].setColor(color);
        matrix[y4][x4].setColor(color);
    }
    public boolean checkOverlap(Block[][] matrix){
        if(matrix[y1][x1].show) return true;
        if(matrix[y2][x2].show) return true;
        if(matrix[y3][x3].show) return true;
        if(matrix[y4][x4].show) return true;
        return false;
    }
}