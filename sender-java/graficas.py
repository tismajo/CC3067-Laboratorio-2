import argparse
import csv
from collections import defaultdict
from pathlib import Path

import matplotlib.pyplot as plt
import numpy as np


ALGORITHMS = ("HAMMING_12_8", "CRC32_IEEE")
ALGORITHM_LABELS = {
    "HAMMING_12_8": "Hamming(12,8)",
    "CRC32_IEEE": "CRC-32",
}
COLORS = {
    "HAMMING_12_8": "#2563EB",
    "CRC32_IEEE": "#DC2626",
}
REQUIRED_COLUMNS = {
    "algorithm",
    "message_bytes",
    "ber",
    "repetition",
    "original_bits",
    "encoded_bits",
    "overhead_bits",
    "altered_bits",
    "connection_success",
    "valid",
    "error_detected",
    "error_corrected",
    "corrected_bits",
    "message_correct",
    "error",
}


def parse_bool(value):
    normalized = value.strip().lower()
    if normalized not in {"true", "false"}:
        raise ValueError(f"Valor booleano inválido: {value!r}")
    return normalized == "true"


def load_results(csv_path):
    with csv_path.open("r", encoding="utf-8-sig", newline="") as stream:
        reader = csv.DictReader(stream)
        columns = set(reader.fieldnames or [])
        missing = REQUIRED_COLUMNS - columns
        if missing:
            raise ValueError(
                "Faltan columnas en el CSV: " + ", ".join(sorted(missing))
            )

        rows = []
        for line_number, raw in enumerate(reader, start=2):
            try:
                row = {
                    "algorithm": raw["algorithm"],
                    "message_bytes": int(raw["message_bytes"]),
                    "ber": float(raw["ber"]),
                    "repetition": int(raw["repetition"]),
                    "original_bits": int(raw["original_bits"]),
                    "encoded_bits": int(raw["encoded_bits"]),
                    "overhead_bits": int(raw["overhead_bits"]),
                    "altered_bits": int(raw["altered_bits"]),
                    "connection_success": parse_bool(raw["connection_success"]),
                    "valid": parse_bool(raw["valid"]),
                    "error_detected": parse_bool(raw["error_detected"]),
                    "error_corrected": parse_bool(raw["error_corrected"]),
                    "corrected_bits": int(raw["corrected_bits"]),
                    "message_correct": parse_bool(raw["message_correct"]),
                    "error": raw["error"],
                }
            except (TypeError, ValueError) as error:
                raise ValueError(f"Fila {line_number} inválida: {error}") from error
            rows.append(row)

    if not rows:
        raise ValueError("El CSV no contiene resultados")
    return rows


def validate_results(rows):
    bad_connections = [row for row in rows if not row["connection_success"]]
    if bad_connections:
        raise ValueError(
            f"Hay {len(bad_connections)} transmisiones con fallo de conexión"
        )

    unexpected_algorithms = sorted(
        {row["algorithm"] for row in rows} - set(ALGORITHMS)
    )
    if unexpected_algorithms:
        raise ValueError(
            "Algoritmos desconocidos: " + ", ".join(unexpected_algorithms)
        )

    zero_ber_failures = [
        row for row in rows if row["ber"] == 0 and not row["message_correct"]
    ]
    if zero_ber_failures:
        raise ValueError(
            f"Hay {len(zero_ber_failures)} mensajes incorrectos con BER igual a cero"
        )


def percentage(numerator, denominator):
    return 0.0 if denominator == 0 else 100.0 * numerator / denominator


def summarize(rows):
    grouped = defaultdict(list)
    for row in rows:
        key = (row["algorithm"], row["message_bytes"], row["ber"])
        grouped[key].append(row)

    summary = []
    for (algorithm, message_bytes, ber), group in sorted(grouped.items()):
        total = len(group)
        altered = [row for row in group if row["altered_bits"] > 0]
        accepted_incorrect = [
            row for row in group if row["valid"] and not row["message_correct"]
        ]
        recovered_altered = [
            row for row in altered if row["message_correct"]
        ]
        detected_altered = [
            row for row in altered if row["error_detected"]
        ]
        first = group[0]

        summary.append(
            {
                "algorithm": algorithm,
                "message_bytes": message_bytes,
                "ber": ber,
                "transmissions": total,
                "altered_frames": len(altered),
                "correct_messages": sum(row["message_correct"] for row in group),
                "correct_rate": percentage(
                    sum(row["message_correct"] for row in group), total
                ),
                "detection_rate_altered": percentage(
                    len(detected_altered), len(altered)
                ),
                "recovery_rate_altered": percentage(
                    len(recovered_altered), len(altered)
                ),
                "accepted_incorrect": len(accepted_incorrect),
                "accepted_incorrect_rate": percentage(
                    len(accepted_incorrect), total
                ),
                "average_altered_bits": sum(
                    row["altered_bits"] for row in group
                ) / total,
                "original_bits": first["original_bits"],
                "encoded_bits": first["encoded_bits"],
                "overhead_bits": first["overhead_bits"],
                "overhead_percent": percentage(
                    first["overhead_bits"], first["original_bits"]
                ),
            }
        )
    return summary


def lookup(summary):
    return {
        (row["algorithm"], row["message_bytes"], row["ber"]): row
        for row in summary
    }


def setup_style():
    style = (
        "seaborn-v0_8-whitegrid"
        if "seaborn-v0_8-whitegrid" in plt.style.available
        else "default"
    )
    plt.style.use(style)
    plt.rcParams.update(
        {
            "font.size": 10,
            "axes.titleweight": "bold",
            "axes.titlesize": 11,
            "figure.titlesize": 14,
            "figure.titleweight": "bold",
        }
    )


def save_figure(fig, output_path):
    fig.savefig(output_path, dpi=300, bbox_inches="tight", facecolor="white")
    plt.close(fig)


def plot_correct_rate(summary, output_dir):
    table = lookup(summary)
    sizes = sorted({row["message_bytes"] for row in summary})
    bers = sorted({row["ber"] for row in summary})
    x = np.arange(len(bers))
    labels = ["0" if ber == 0 else f"{ber:g}" for ber in bers]

    fig, axes = plt.subplots(1, len(sizes), figsize=(14, 4.5), sharey=True)
    for axis, size in zip(np.atleast_1d(axes), sizes):
        for algorithm in ALGORITHMS:
            values = [table[(algorithm, size, ber)]["correct_rate"] for ber in bers]
            axis.plot(
                x,
                values,
                marker="o",
                linewidth=2,
                label=ALGORITHM_LABELS[algorithm],
                color=COLORS[algorithm],
            )
        axis.set_title(f"Mensaje de {size} bytes")
        axis.set_xticks(x, labels)
        axis.set_xlabel("BER")
        axis.set_ylim(-3, 103)
        axis.grid(True, linestyle="--", alpha=0.45)

    axes[0].set_ylabel("Mensajes correctos (%)")
    axes[-1].legend(loc="best")
    fig.suptitle("Tasa experimental de mensajes recibidos correctamente")
    fig.tight_layout()
    save_figure(fig, output_dir / "01_tasa_mensajes_correctos.png")


def plot_effectiveness(summary, output_dir):
    table = lookup(summary)
    sizes = sorted({row["message_bytes"] for row in summary})
    bers = sorted({row["ber"] for row in summary if row["ber"] > 0})
    x = np.arange(len(bers))
    labels = [f"{ber:g}" for ber in bers]

    fig, axes = plt.subplots(1, len(sizes), figsize=(14, 4.5), sharey=True)
    for axis, size in zip(np.atleast_1d(axes), sizes):
        crc_detection = [
            table[("CRC32_IEEE", size, ber)]["detection_rate_altered"]
            for ber in bers
        ]
        hamming_recovery = [
            table[("HAMMING_12_8", size, ber)]["recovery_rate_altered"]
            for ber in bers
        ]
        axis.plot(
            x,
            crc_detection,
            marker="o",
            linewidth=2,
            label="CRC-32: deteccion",
            color=COLORS["CRC32_IEEE"],
        )
        axis.plot(
            x,
            hamming_recovery,
            marker="s",
            linewidth=2,
            label="Hamming: recuperacion",
            color=COLORS["HAMMING_12_8"],
        )
        axis.set_title(f"Mensaje de {size} bytes")
        axis.set_xticks(x, labels)
        axis.set_xlabel("BER")
        axis.set_ylim(-3, 103)
        axis.grid(True, linestyle="--", alpha=0.45)

    axes[0].set_ylabel("Tramas alteradas gestionadas (%)")
    axes[-1].legend(loc="best")
    fig.suptitle("Deteccion de CRC-32 y recuperacion de Hamming(12,8)")
    fig.text(
        0.5,
        -0.02,
        "CRC-32: errores detectados entre tramas alteradas. "
        "Hamming: mensajes recuperados correctamente entre tramas alteradas.",
        ha="center",
        fontsize=9,
    )
    fig.tight_layout()
    save_figure(fig, output_dir / "02_deteccion_y_recuperacion.png")


def plot_overhead(summary, output_dir):
    table = lookup(summary)
    sizes = sorted({row["message_bytes"] for row in summary})
    representative_ber = min(row["ber"] for row in summary)

    fig, axis = plt.subplots(figsize=(8, 5))
    for algorithm in ALGORITHMS:
        values = [
            table[(algorithm, size, representative_ber)]["overhead_percent"]
            for size in sizes
        ]
        axis.plot(
            sizes,
            values,
            marker="o",
            linewidth=2,
            label=ALGORITHM_LABELS[algorithm],
            color=COLORS[algorithm],
        )

    axis.set_xscale("log")
    axis.set_xticks(sizes, [str(size) for size in sizes])
    axis.set_xlabel("Tamaño del mensaje original (bytes)")
    axis.set_ylabel("Overhead de redundancia (%)")
    axis.set_ylim(0, 55)
    axis.set_title("Overhead según el tamaño del mensaje")
    axis.legend()
    axis.grid(True, which="both", linestyle="--", alpha=0.45)
    fig.tight_layout()
    save_figure(fig, output_dir / "03_overhead.png")


def plot_incorrect_acceptance(summary, output_dir):
    table = lookup(summary)
    sizes = sorted({row["message_bytes"] for row in summary})
    bers = sorted({row["ber"] for row in summary if row["ber"] > 0})
    x = np.arange(len(sizes))
    width = 0.24

    fig, axis = plt.subplots(figsize=(9, 5))
    for index, ber in enumerate(bers):
        values = [
            table[("HAMMING_12_8", size, ber)]["accepted_incorrect_rate"]
            for size in sizes
        ]
        offset = (index - (len(bers) - 1) / 2) * width
        bars = axis.bar(x + offset, values, width, label=f"BER = {ber:g}")
        axis.bar_label(bars, fmt="%.0f%%", padding=2, fontsize=8)

    axis.set_xticks(x, [f"{size} bytes" for size in sizes])
    axis.set_xlabel("Tamaño del mensaje original")
    axis.set_ylabel("Mensajes incorrectos aceptados (%)")
    axis.set_ylim(0, 32)
    axis.set_title("Limitación de Hamming(12,8) ante errores múltiples")
    axis.legend()
    axis.grid(True, axis="y", linestyle="--", alpha=0.45)
    fig.tight_layout()
    save_figure(fig, output_dir / "04_aceptaciones_incorrectas_hamming.png")


def write_summary(summary, output_path):
    columns = [
        "algorithm",
        "message_bytes",
        "ber",
        "transmissions",
        "altered_frames",
        "correct_messages",
        "correct_rate",
        "detection_rate_altered",
        "recovery_rate_altered",
        "accepted_incorrect",
        "accepted_incorrect_rate",
        "average_altered_bits",
        "original_bits",
        "encoded_bits",
        "overhead_bits",
        "overhead_percent",
    ]
    with output_path.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=columns)
        writer.writeheader()
        writer.writerows(summary)


def main():
    parser = argparse.ArgumentParser(
        description="Genera gráficas a partir del experimento de laboratorio 2."
    )
    parser.add_argument(
        "csv",
        nargs="?",
        default="resultados_experimento_final.csv",
        help="Ruta del CSV generado por ExperimentRunner",
    )
    parser.add_argument(
        "--output-dir",
        default="graficas_laboratorio2",
        help="Directorio donde se guardarán las gráficas",
    )
    args = parser.parse_args()

    csv_path = Path(args.csv)
    output_dir = Path(args.output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    rows = load_results(csv_path)
    validate_results(rows)
    summary = summarize(rows)

    setup_style()
    plot_correct_rate(summary, output_dir)
    plot_effectiveness(summary, output_dir)
    plot_overhead(summary, output_dir)
    plot_incorrect_acceptance(summary, output_dir)
    write_summary(summary, output_dir / "resumen_resultados.csv")

    print(f"Filas analizadas: {len(rows)}")
    print(f"Gráficas y resumen guardados en: {output_dir.resolve()}")


if __name__ == "__main__":
    main()
