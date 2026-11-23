package co.finanzas.aplicacion.puertos.entrada;

import co.finanzas.dominio.modelo.Gasto;

public interface IEvaluadorPresupuesto {
    void evaluar(Gasto gasto);
}
