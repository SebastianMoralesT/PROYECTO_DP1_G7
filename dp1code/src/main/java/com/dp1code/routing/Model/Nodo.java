package com.dp1code.routing.Model;

import java.util.Objects;

public class Nodo {
    private int posX;
    private int posY;
    private boolean bloqueado;

    public Nodo() {}

    public Nodo(int posX, int posY) {
        this.posX = posX;
        this.posY = posY;
    }

    public int getPosX() { return posX; }
    public void setPosX(int posX) { this.posX = posX; }

    public int getPosY() { return posY; }
    public void setPosY(int posY) { this.posY = posY; }

    public boolean isBloqueado() { return bloqueado; }
    public void setBloqueado(boolean bloqueado) { this.bloqueado = bloqueado; }

    @Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Nodo nodo = (Nodo) o;
    return posX == nodo.posX && posY == nodo.posY;
}

@Override
public int hashCode() {
    return Objects.hash(posX, posY);
}
}