package com.ainzzorl.algorithms.images

import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Option
import org.apache.commons.cli.Options
import org.apache.commons.cli.ParseException
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.system.exitProcess

object SeamCarving {
    @JvmStatic
    fun main(args: Array<String>) {
        val options = Options()
        val input = Option("i", "input", true, "input file path")
        input.isRequired = true
        options.addOption(input)

        val output = Option("o", "output", true, "output file")
        output.isRequired = true
        options.addOption(output)

        val outputHeight = Option("h", "output-height", true, "output height")
        outputHeight.isRequired = false
        options.addOption(outputHeight)

        val outputWidth = Option("w", "output-width", true, "output width")
        outputWidth.isRequired = false
        options.addOption(outputWidth)

        val artifacts = Option("a", "artifacts", true, "directory to store artifacts")
        artifacts.isRequired = false
        options.addOption(artifacts)

        val parser = DefaultParser()
        val formatter = HelpFormatter()
        val cmd: CommandLine

        try {
            cmd = parser.parse(options, args);
        } catch (e: ParseException) {
            println(e.message);
            formatter.printHelp("seam-carving", options);
            exitProcess(1);
        }

        val inputFilePath = cmd.getOptionValue("input")
        val outputFilePath = cmd.getOptionValue("output")
        val storeArtifacts = cmd.hasOption("artifacts")
        val artifactsPath = cmd.getOptionValue("artifacts")

        val original: BufferedImage = ImageIO.read(File(inputFilePath))

        val targetWidth = if (cmd.hasOption("output-width")) {
            cmd.getOptionValue("output-width").toInt()
        } else {
            original.width
        }

        val targetHeight = if (cmd.hasOption("output-height")) {
            cmd.getOptionValue("output-height").toInt()
        } else {
            original.height
        }

        val fixedVertical = carveVertically(original, targetWidth, storeArtifacts, artifactsPath, false, 0)
        // We implement horizontal carving by transposing the image and doing vertical carving
        val fixedHorizontal = carveVertically(ImageUtils.transpose(fixedVertical), targetHeight, storeArtifacts, artifactsPath, true, original.width - targetWidth)
        val result = ImageUtils.transpose(fixedHorizontal)
        ImageIO.write(result, "jpg", File(outputFilePath))
    }

    private fun carveVertically(image: BufferedImage, targetWidth: Int, storeArtifacts: Boolean, artifactsPath: String, transpose: Boolean, iterationStart: Int): BufferedImage {
        var current = image
        var grey: BufferedImage = EnergyMap.toGrey(image)

        repeat(image.width - targetWidth) { i ->
            val energyMap = EnergyMap.toEnergyMap(grey)
            val seam = getLowEnergyVerticalSeam(energyMap)
            val previous = current
            current = removeSeam(current, seam)
            grey = removeSeam(grey, seam)
            if (storeArtifacts) {
                val iterationPrefix = (i + iterationStart).toString().padStart(5, '0')

                val outputPaintedSeam = if (transpose) ImageUtils.transpose(paintVerticalSeam(previous, seam)) else paintVerticalSeam(previous, seam)
                ImageIO.write(outputPaintedSeam, "jpg", File("$artifactsPath/iteration-${iterationPrefix}-seam.jpg"))
                val outputCurrent = if (transpose) ImageUtils.transpose(current) else current
                ImageIO.write(outputCurrent, "jpg", File("$artifactsPath/iteration-${iterationPrefix}-wip.jpg"))
            }
        }
        return current
    }


    private fun getLowEnergyVerticalSeam(energyMap: BufferedImage): IntArray {
        val dp = Array(energyMap.width) { IntArray(energyMap.height) }

        for (x in 0 until energyMap.width) {
            dp[x][0] = energyMap.getRGB(x, 0)
        }
        for (y in 1 until energyMap.height) {
            for (x in 0 until energyMap.width) {
                dp[x][y] = dp[x][y - 1]
                if (x > 0 && dp[x - 1][y - 1] < dp[x][y]) {
                    dp[x][y] = dp[x - 1][y - 1]
                }
                if (x < energyMap.width - 1 && dp[x + 1][y - 1] < dp[x][y]) {
                    dp[x][y] = dp[x + 1][y - 1]
                }
                dp[x][y] += energyMap.getRGB(x, y)
            }
        }

        val result = IntArray(energyMap.height)
        var bestX = 0
        for (x in 1 until energyMap.width) {
            if (dp[x][energyMap.height - 1] < dp[bestX][energyMap.height - 1]) {
                bestX = x
            }
        }
        var x = bestX
        for (y in energyMap.height - 1 downTo 0) {
            result[y] = x
            if (y > 0) {
                var newX = x
                if (x > 0 && dp[x - 1][y - 1] < dp[x][y - 1]) {
                    newX = x - 1
                }
                if (x < energyMap.width - 1 && dp[x + 1][y - 1] < dp[x][y - 1]) {
                    newX = x + 1
                }
                x = newX
            }
        }

        return result
    }

    private fun paintVerticalSeam(image: BufferedImage, seam: IntArray): BufferedImage {
        val result = ImageUtils.deepCopy(image)
        for (y in 0 until image.height) {
            result.setRGB(seam[y], y, 0xFF0000) // Red
        }
        return result
    }

    private fun removeSeam(image: BufferedImage, seam: IntArray): BufferedImage {
        val result = BufferedImage(image.width - 1, image.height, image.type)
        for (y in 0 until image.height) {
            for (x in 0 until seam[y]) {
                result.setRGB(x, y, image.getRGB(x, y))
            }
            for (x in seam[y] until image.width - 1) {
                result.setRGB(x, y, image.getRGB(x + 1, y))
            }
        }
        return result
    }
}