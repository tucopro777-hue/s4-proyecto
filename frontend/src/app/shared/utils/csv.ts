/**
 * Genera un archivo CSV y lo descarga en el navegador.
 *
 * - Lleva BOM UTF-8 para que Excel muestre bien tildes y eñes.
 * - El separador sigue la configuración regional: donde el decimal es coma (es-BO, es-ES...)
 *   Excel espera ";" como separador de columnas; donde es punto, espera ",".
 * - Los números se escriben con el decimal regional para que Excel los trate como números.
 */
export function exportarCsv(nombreArchivo: string, encabezados: string[], filas: (string | number)[][]): void {
  const decimal = (1.1).toLocaleString().includes(',') ? ',' : '.';
  const separador = decimal === ',' ? ';' : ',';

  const celda = (valor: string | number): string => {
    if (typeof valor === 'number') {
      return valor.toFixed(2).replace('.', decimal);
    }
    // Un texto que empieza por = + - @ se interpretaría como fórmula en Excel: se neutraliza
    const texto = /^[=+\-@]/.test(valor) ? `'${valor}` : valor;
    return `"${texto.replace(/"/g, '""')}"`;
  };

  const lineas = [encabezados, ...filas].map((fila) => fila.map(celda).join(separador));
  const blob = new Blob(['\uFEFF' + lineas.join('\r\n')], { type: 'text/csv;charset=utf-8' });

  const enlace = document.createElement('a');
  enlace.href = URL.createObjectURL(blob);
  enlace.download = nombreArchivo;
  enlace.click();
  URL.revokeObjectURL(enlace.href);
}
