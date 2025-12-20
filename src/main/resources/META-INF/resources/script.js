var ratioChart;
var gridGauge;
var evGauge;
var pvGauge;
var usageGauge;
var monthPeakGauge;
var currentAverageGauge;
var newAverageEstimateGauge;



function refreshData() {
        $.get( "/metrics/live", function( d ) {
        var totalUsageWithoutEV = (d.importW - d.exportW + d.pvW - d.evW);
        const data = {
          datasets: [{
            label: '',
            data: [d.pvW, d.importW],
            backgroundColor: [
              '#43de66',
              '#e3110e'
            ]
          },
          {
            label: '',
            data: [d.evW, totalUsageWithoutEV, d.exportW ],
            backgroundColor: [
              '#25a5f5',
              '#deadff',
              '#cfcfcf'
            ]
          }]
        };
        ratioChart.data = data;
        ratioChart.update();
        evGauge.refresh(d.evW);
        pvGauge.refresh(d.pvW);
        monthPeakGauge.refresh(d.monthlyPowerPeakW);
        currentAverageGauge.refresh(d.importAverageW);
        newAverageEstimateGauge.refresh(d.peakEstimateW);
        usageGauge.refresh(totalUsageWithoutEV);
        if (d.importW > 0) {
             gridGauge.refresh(d.importW);
        } else {
            gridGauge.refresh(-d.exportW);
        }
        if (d.evConnected) {
            $("#evconnected").show();
            $("#evnotconnected").hide();
            $("#evbatterylevel").html(d.evConnectedName+" "+d.evBatteryLevel+" %");
        } else {
            $("#evconnected").hide();
            $("#evnotconnected").show();
            $("#evbatterylevel").html("");
        }
        {
        let tsString = d.timestamp;
        let tIndex = tsString.indexOf("T");
        let dotIndex = tsString.indexOf(".");
        let dts = $.datepicker.parseDate( "yy-mm-dd", tsString.substring(0, tIndex) );
        $("#livedatatimestamp").html($.datepicker.formatDate("dd/mm/yy", dts) + " " + tsString.substring(tIndex + 1, dotIndex));
        }

        {
        let tsString = d.monthlyPowerPeakTimestamp;
        let tIndex = tsString.indexOf("T");
        let dotIndex = tsString.indexOf("+");
        let dts = $.datepicker.parseDate( "yy-mm-dd", tsString.substring(0, tIndex) );
        $("#monthPeakDate").html($.datepicker.formatDate("dd/mm/yy", dts) + " " + tsString.substring(tIndex + 1, dotIndex));
        }
    });
}

$(function () {
    const ctx = document.getElementById('ratioChart');
    const data = {
      datasets: [{
        label: '',
        data: [1],
        backgroundColor: [
          '#cfcfcf',
        ]
      },
      {
        label: '',
        data: [1],
        backgroundColor: [
          '#cfcfcf'
        ]
      }]
    };

    const config = {
        type: 'doughnut',
        data: data,
        options: {
            animation: { animateRotate: false} ,
            responsive: true,
            plugins: {
              title: {
                display: true,
                text: 'IN / OUT ratio'
            }
        }
      },
    };

    ratioChart = new Chart(ctx, config);

    gridGauge = new JustGage({
        id: "gridGauge", // the id of the html element
        value: 0,
        min: -4000,
        max: 4000,
        symbol: ' W',
        label: "Current grid meter",
        decimals: 0,
        gaugeWidthScale: 0.6,
        differential: true,
        labelMinFontSize: 14,
        minLabelMinFontSize: 14,
        maxLabelMinFontSize: 14,
        pointer: true,
        pointerOptions: {
          toplength: -15,
          bottomlength: 50,
          bottomwidth: 10,
          color: '#8e8e93',
          stroke: '#ffffff',
          stroke_width: 1,
          stroke_linecap: 'round'
        }
      });
    evGauge = new JustGage({
        id: "evGauge",
        value: 0,
        min: 0,
        max: 7400,
        symbol: ' W',
        label: "EV Charging",
        decimals: 0,
        gaugeWidthScale: 0.6,
        pointer: true,
        levelColorsGradient: true,
        labelMinFontSize: 14,
        minLabelMinFontSize: 14,
        maxLabelMinFontSize: 14,
        levelColors: ["#8fb8f7", "#66a1fa", "#1772fc"],
        pointerOptions: {
          toplength: -15,
          bottomlength: 50,
          bottomwidth: 10,
          color: '#8e8e93',
          stroke: '#ffffff',
          stroke_width: 1,
          stroke_linecap: 'round'
        }
    });

    pvGauge = new JustGage({
        id: "pvGauge",
        value: 0,
        min: 0,
        max: 5000,
        symbol: ' W',
        label: "PV Yield",
        decimals: 0,
        gaugeWidthScale: 0.6,
        pointer: true,
        labelMinFontSize: 14,
        minLabelMinFontSize: 14,
        maxLabelMinFontSize: 14,
        levelColorsGradient: true,
        levelColors: ["#b1f7a6", "#88f777", "#55fa3c"],
        pointerOptions: {
          toplength: -15,
          bottomlength: 50,
          bottomwidth: 10,
          color: '#8e8e93',
          stroke: '#ffffff',
          stroke_width: 1,
          stroke_linecap: 'round'
        }
    });

    usageGauge = new JustGage({
        id: "usageGauge",
        value: 0,
        min: 0,
        max: 5000,
        symbol: ' W',
        label: "Home Usage",
        decimals: 0,
        gaugeWidthScale: 0.6,
        pointer: true,
        labelMinFontSize: 14,
        minLabelMinFontSize: 14,
        maxLabelMinFontSize: 14,
        levelColorsGradient: true,
        levelColors: ["#deadff", "#deadff", "#deadff"],
        pointerOptions: {
          toplength: -15,
          bottomlength: 50,
          bottomwidth: 10,
          color: '#8e8e93',
          stroke: '#ffffff',
          stroke_width: 1,
          stroke_linecap: 'round'
        }
      });

    currentAverageGauge = new JustGage({
        id: "currentAverageGauge",
        value: 0,
        min: 0,
        max: 6000,
        symbol: ' W',
        label: "Current 15m average",
        decimals: 0,
        gaugeWidthScale: 0.6,
        pointer: true,
        labelMinFontSize: 14,
        minLabelMinFontSize: 14,
        maxLabelMinFontSize: 14,
        levelColorsGradient: true,
        levelColors: ["#a5fc03", "#fcf403", "#fcce03", "#fc6f03"],
        pointerOptions: {
          toplength: -15,
          bottomlength: 50,
          bottomwidth: 10,
          color: '#8e8e93',
          stroke: '#ffffff',
          stroke_width: 1,
          stroke_linecap: 'round'
        }
      });


    monthPeakGauge = new JustGage({
        id: "monthPeakGauge",
        value: 0,
        min: 0,
        max: 6000,
        symbol: ' W',
        label: "Monthly peak",
        decimals: 0,
        gaugeWidthScale: 0.6,
        pointer: true,
        labelMinFontSize: 14,
        minLabelMinFontSize: 14,
        maxLabelMinFontSize: 14,
        levelColorsGradient: true,
        levelColors: ["#a5fc03", "#fcf403", "#fcce03", "#fc6f03"],
        pointerOptions: {
          toplength: -15,
          bottomlength: 50,
          bottomwidth: 10,
          color: '#8e8e93',
          stroke: '#ffffff',
          stroke_width: 1,
          stroke_linecap: 'round'
        }
      });

    newAverageEstimateGauge = new JustGage({
        id: "newAverageEstimateGauge",
        value: 0,
        min: 0,
        max: 6000,
        symbol: ' W',
        label: "Estimate 15m peak",
        decimals: 0,
        gaugeWidthScale: 0.6,
        pointer: true,
        labelMinFontSize: 14,
        minLabelMinFontSize: 14,
        maxLabelMinFontSize: 14,
        levelColorsGradient: true,
        levelColors: ["#a5fc03", "#fcf403", "#fcce03", "#fc6f03"],
        pointerOptions: {
          toplength: -15,
          bottomlength: 50,
          bottomwidth: 10,
          color: '#8e8e93',
          stroke: '#ffffff',
          stroke_width: 1,
          stroke_linecap: 'round'
        }
      });


    refreshData();
    setInterval(refreshData, 15000);
});